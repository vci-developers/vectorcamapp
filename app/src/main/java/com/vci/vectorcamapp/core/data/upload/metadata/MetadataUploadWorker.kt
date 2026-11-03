package com.vci.vectorcamapp.core.data.upload.metadata

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.data.dto.device.DeviceDto
import com.vci.vectorcamapp.core.data.dto.form_answer.FormAnswerDto
import com.vci.vectorcamapp.core.data.dto.inference_result.InferenceResultDto
import com.vci.vectorcamapp.core.data.dto.session.SessionDto
import com.vci.vectorcamapp.core.data.dto.session_unit.SessionUnitDto
import com.vci.vectorcamapp.core.data.dto.specimen.SpecimenDto
import com.vci.vectorcamapp.core.data.dto.specimen_image.SpecimenImageDto
import com.vci.vectorcamapp.core.data.dto.surveillance_form.SurveillanceFormDto
import com.vci.vectorcamapp.core.data.mappers.toCameraMetadata
import com.vci.vectorcamapp.core.data.mappers.toImageMetadataDto
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.data.upload.logging.UploadErrorLogger
import com.vci.vectorcamapp.core.data.upload.logging.UploadTrace
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.network.api.DeviceDataSource
import com.vci.vectorcamapp.core.domain.network.api.FormAnswerDataSource
import com.vci.vectorcamapp.core.domain.network.api.SessionDataSource
import com.vci.vectorcamapp.core.domain.network.api.SessionUnitDataSource
import com.vci.vectorcamapp.core.domain.network.api.SpecimenDataSource
import com.vci.vectorcamapp.core.domain.network.api.SpecimenImageDataSource
import com.vci.vectorcamapp.core.domain.network.api.SurveillanceFormDataSource
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.repository.InferenceResultRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.SurveillanceFormRepository
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.core.presentation.util.error.toString
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import java.util.UUID
import androidx.work.ListenableWorker.Result as WorkerResult
import com.vci.vectorcamapp.core.domain.util.Result as DomainResult

@HiltWorker
class MetadataUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val deviceCache: DeviceCache,
    private val programRepository: ProgramRepository,
    private val transactionHelper: TransactionHelper,
    private val sessionRepository: SessionRepository,
    private val sessionUnitRepository: SessionUnitRepository,
    private val surveillanceFormRepository: SurveillanceFormRepository,
    private val specimenRepository: SpecimenRepository,
    private val specimenImageRepository: SpecimenImageRepository,
    private val inferenceResultRepository: InferenceResultRepository,
    private val deviceDataSource: DeviceDataSource,
    private val sessionDataSource: SessionDataSource,
    private val sessionUnitDataSource: SessionUnitDataSource,
    private val surveillanceFormDataSource: SurveillanceFormDataSource,
    private val specimenDataSource: SpecimenDataSource,
    private val specimenImageDataSource: SpecimenImageDataSource,
    private val formAnswerDataSource: FormAnswerDataSource,
    private val formAnswerRepository: FormAnswerRepository,
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val MAX_RETRIES = 5
        const val CHANNEL_ID = "metadata_upload_channel"
    }

    private var notificationId = 1001
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var activeTrace: UploadTrace? = null

    override suspend fun doWork(): WorkerResult {
        val trace = UploadTrace(worker = "MetadataUploadWorker")
        activeTrace = trace
        return try {
            withContext(trace) {
        val localSessionIdString = inputData.getString("session_id")
        val localSiteId = inputData.getInt("site_id", -1)
        if (localSessionIdString == null || localSiteId == -1) {
            return@withContext retryOrFailure(
                message = context.getString(R.string.upload_error_invalid_request),
                detail = "Invalid metadata upload request sessionId=$localSessionIdString siteId=$localSiteId",
            )
        }

        val localSessionId = try {
            UUID.fromString(localSessionIdString)
        } catch (e: IllegalArgumentException) {
            return@withContext retryOrFailure(
                message = context.getString(R.string.upload_error_invalid_session_id),
                detail = "Invalid session id '$localSessionIdString'",
                throwable = e,
            )
        }
        activeTrace?.sessionId = localSessionId.toString()
        activeTrace?.siteId = localSiteId.toString()

        val localSession = sessionRepository.getSessionById(localSessionId)
        val localDevice = deviceCache.getDevice()
        val localProgramId = deviceCache.getProgramId()
        activeTrace?.programId = localProgramId?.toString()
        if (localSession == null || localDevice == null || localProgramId == null) {
            return@withContext retryOrFailure(
                message = context.getString(R.string.upload_error_device_session_not_found),
                detail = "Missing local data session=${localSession != null} device=${localDevice != null} " +
                    "program=${localProgramId != null} deviceId=${localDevice?.id}",
            )
        }

        val localProgram = programRepository.getProgramById(localProgramId)
            ?: return@withContext retryOrFailure(context.getString(R.string.upload_error_program_not_found))

        createNotificationChannel()
        notificationId = localSessionId.hashCode()
        setForeground(showInitialMetadataNotification())

        try {
            val syncedDevice =
                when (val syncDeviceResult = syncDeviceIfNeeded(localDevice, localProgramId)) {
                    is DomainResult.Success -> syncDeviceResult.data
                    is DomainResult.Error -> return@withContext retryOrFailure(
                        syncDeviceResult.error.toString(context)
                    )
                }

            val localSpecimensWithImagesAndInferenceResults =
                specimenRepository.getSpecimenImagesAndInferenceResultsBySessionScope(
                    localSessionId, null
                )
            val totalSpecimensForSession = localSpecimensWithImagesAndInferenceResults.size

            val syncedSession = when (val syncSessionResult = syncSessionIfNeeded(
                localSession, localSiteId, syncedDevice.id, totalSpecimensForSession
            )) {
                is DomainResult.Success -> syncSessionResult.data
                is DomainResult.Error -> return@withContext retryOrFailure(
                    syncSessionResult.error.toString(context)
                )
            }
            if (syncedSession.remoteId == null) {
                return@withContext retryOrFailure(context.getString(R.string.upload_error_session_not_found_server))
            }

            val syncedSessionUnitsByLocalId: Map<UUID, SessionUnit> =
                if (localProgram.formVersion != null) {
                    val localSessionUnits =
                        sessionUnitRepository.getSessionUnitsForSession(syncedSession.localId)
                    buildMap {
                        localSessionUnits.forEach { localSessionUnit ->
                            when (val syncSessionUnitResult = syncSessionUnitIfNeeded(
                                localSessionUnit, syncedSession.localId, syncedSession.remoteId
                            )) {
                                is DomainResult.Success -> put(
                                    localSessionUnit.localId, syncSessionUnitResult.data
                                )

                                is DomainResult.Error -> return@withContext retryOrFailure(
                                    syncSessionUnitResult.error.toString(context)
                                )
                            }
                        }
                    }
                } else {
                    emptyMap()
                }

            if (localProgram.formVersion != null) {
                val sessionScopedFormAnswers =
                    formAnswerRepository.getSessionScopedFormAnswers(syncedSession.localId)
                val sessionUnitScopedFormAnswersBySessionUnit =
                    syncedSessionUnitsByLocalId.keys.associateWith { localUnitId ->
                        formAnswerRepository.getSessionUnitScopedFormAnswers(localUnitId)
                    }.filterValues { it.isNotEmpty() }

                if (sessionScopedFormAnswers.isNotEmpty() || sessionUnitScopedFormAnswersBySessionUnit.isNotEmpty()) {
                    activeTrace?.step = "sync_form_answers"
                    when (val syncFormAnswersResult = syncFormAnswersIfNeeded(
                        sessionScopedFormAnswers,
                        sessionUnitScopedFormAnswersBySessionUnit,
                        syncedSessionUnitsByLocalId,
                        syncedSession.localId,
                        syncedSession.remoteId,
                    )) {
                        is DomainResult.Success -> syncFormAnswersResult.data
                        is DomainResult.Error -> return@withContext retryOrFailure(
                            syncFormAnswersResult.error.toString(context)
                        )
                    }
                }
            } else {
                val localSurveillanceForm =
                    surveillanceFormRepository.getSurveillanceFormBySessionId(syncedSession.localId)
                if (localSurveillanceForm != null) {
                    when (val syncSurveillanceFormResult = syncSurveillanceFormIfNeeded(
                        localSurveillanceForm, syncedSession.localId, syncedSession.remoteId
                    )) {
                        is DomainResult.Success -> syncSurveillanceFormResult.data
                        is DomainResult.Error -> return@withContext retryOrFailure(
                            syncSurveillanceFormResult.error.toString(context)
                        )
                    }
                }
            }

            var hasFailure = false
            localSpecimensWithImagesAndInferenceResults.forEachIndexed { specimenIndex, specimenWithImagesAndInferenceResults ->
                val totalImagesForSpecimen =
                    specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.size

                val syncedLocalSessionUnitId = specimenRepository.getSessionUnitIdForSpecimen(
                    specimenWithImagesAndInferenceResults.specimen.id,
                    syncedSession.localId,
                )
                val syncedRemoteSessionUnitId = syncedLocalSessionUnitId?.let {
                    syncedSessionUnitsByLocalId[it]?.remoteId
                }

                val syncedSpecimen = when (val syncSpecimenResult = syncSpecimenIfNeeded(
                    specimenWithImagesAndInferenceResults.specimen,
                    syncedSession.localId,
                    syncedSession.remoteId,
                    syncedLocalSessionUnitId,
                    syncedRemoteSessionUnitId,
                    totalImagesForSpecimen,
                )) {
                    is DomainResult.Success -> syncSpecimenResult.data
                    is DomainResult.Error -> {
                        hasFailure = true
                        logUploadFailure(
                            reason = "Specimen metadata was not uploaded for ${specimenWithImagesAndInferenceResults.specimen.id}",
                            networkError = syncSpecimenResult.error,
                        )
                        specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.forEach { (specimenImage, _) ->
                            if (specimenImage.metadataUploadStatus != UploadStatus.COMPLETED) {
                                specimenImageRepository.updateSpecimenImage(
                                    specimenImage.copy(metadataUploadStatus = UploadStatus.FAILED),
                                    specimenWithImagesAndInferenceResults.specimen.id,
                                    syncedSession.localId
                                )
                            }
                        }
                        return@forEachIndexed
                    }
                }

                specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.forEachIndexed { imageIndex, (specimenImage, inferenceResult) ->
                    if (specimenImage.metadataUploadStatus != UploadStatus.COMPLETED) {
                        syncSpecimenImageAndInferenceResultIfNeeded(
                            specimenImage, inferenceResult, syncedSpecimen, syncedSession.localId
                        ).onSuccess {
                            showSpecimenProgressNotification(
                                specimenId = syncedSpecimen.id,
                                currentSpecimenIndex = specimenIndex,
                                totalSpecimens = totalSpecimensForSession,
                                currentImageIndex = imageIndex,
                                totalImagesForSpecimen = totalImagesForSpecimen
                            )
                        }.onError { error ->
                            hasFailure = true
                            logUploadFailure(
                                reason = "Specimen image metadata was not uploaded for ${specimenImage.localId}",
                                networkError = error,
                            )
                            specimenImageRepository.updateSpecimenImage(
                                specimenImage.copy(metadataUploadStatus = UploadStatus.FAILED),
                                syncedSpecimen.id,
                                syncedSession.localId
                            )
                        }
                    }
                }
            }

            return@withContext if (hasFailure) {
                retryOrComplete()
            } else {
                WorkerResult.success()
            }
        } catch (e: IOException) {
            return@withContext retryOrFailure(
                message = context.getString(R.string.upload_error_lost_connection),
                throwable = e,
            )
        } catch (e: Exception) {
            return@withContext retryOrFailure(
                message = context.getString(R.string.upload_error_unknown),
                throwable = e,
            )
        } finally {
            if (isStopped) {
                logUploadFailure(reason = "Metadata upload worker was stopped before it finished")
                resetInProgressUploads(localSession.localId)
            }
        }
            }
        } finally {
            activeTrace = null
        }
    }

    private suspend fun resetInProgressUploads(sessionId: UUID) {
        val specimensWithImagesAndInferenceResults =
            specimenRepository.getSpecimenImagesAndInferenceResultsBySessionScope(sessionId, null)
        specimensWithImagesAndInferenceResults.forEach { specimenWithImagesAndInferenceResults ->
            specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.forEach { (specimenImage, _) ->
                if (specimenImage.metadataUploadStatus == UploadStatus.IN_PROGRESS) {
                    specimenImageRepository.updateSpecimenImage(
                        specimenImage.copy(metadataUploadStatus = UploadStatus.FAILED),
                        specimenWithImagesAndInferenceResults.specimen.id,
                        sessionId
                    )
                }
            }
        }
    }

    private fun retryOrComplete(): WorkerResult {
        showUploadRetryNotification(context.getString(R.string.upload_error_images_failed))
        return if (runAttemptCount < MAX_RETRIES) {
            WorkerResult.retry()
        } else {
            WorkerResult.success()
        }
    }

    private fun retryOrFailure(
        message: String,
        detail: String? = null,
        throwable: Throwable? = null,
    ): WorkerResult {
        logUploadFailure(reason = detail ?: message, throwable = throwable)
        if (runAttemptCount < MAX_RETRIES) {
            showUploadRetryNotification(message)
            return WorkerResult.retry()
        } else {
            showUploadErrorNotification(message)
            return WorkerResult.failure()
        }
    }

    private fun logUploadFailure(
        reason: String,
        networkError: NetworkError? = null,
        throwable: Throwable? = null,
    ) {
        UploadErrorLogger.record(
            trace = activeTrace,
            reason = reason,
            networkError = networkError?.name,
            throwable = throwable,
        )
    }

    private suspend fun refreshRecordedAppVersion(device: DeviceDto): DeviceDto {
        return when (val refreshed = deviceDataSource.updateAppVersionIfStale(device)) {
            is DomainResult.Success -> refreshed.data
            is DomainResult.Error -> {
                logUploadFailure(
                    reason = "Failed to update app version for device ${device.deviceId}. " +
                        "Session upload will continue and registration will not be repeated.",
                    networkError = refreshed.error,
                )
                device
            }
        }
    }

    private suspend fun syncDeviceIfNeeded(
        localDevice: Device, localProgramId: Int
    ): DomainResult<Device, NetworkError> {
        activeTrace?.step = "sync_device"
        activeTrace?.specimenId = null
        activeTrace?.imageId = null
        return try {
            val localDeviceDto = DeviceDto(
                deviceId = localDevice.id,
                model = localDevice.model,
                registeredAt = localDevice.registeredAt,
                submittedAt = localDevice.submittedAt,
                programId = localProgramId
            )

            val remoteDeviceDto =
                when (val remoteDeviceResult = deviceDataSource.getDeviceById(localDevice.id)) {
                    is DomainResult.Success -> refreshRecordedAppVersion(remoteDeviceResult.data)
                    is DomainResult.Error -> {
                        when (remoteDeviceResult.error) {
                            NetworkError.NOT_FOUND -> {
                                val registerDeviceResult =
                                    deviceDataSource.registerDevice(localDevice, localProgramId)
                                when (registerDeviceResult) {
                                    is DomainResult.Success -> registerDeviceResult.data.device
                                    is DomainResult.Error -> return DomainResult.Error(
                                        registerDeviceResult.error
                                    )
                                }
                            }

                            else -> return DomainResult.Error(remoteDeviceResult.error)
                        }
                    }
                }

            val remoteDevice = Device(
                id = remoteDeviceDto.deviceId,
                model = remoteDeviceDto.model,
                registeredAt = remoteDeviceDto.registeredAt,
                submittedAt = remoteDeviceDto.submittedAt
            )

            if (remoteDeviceDto.copy(appVersion = null) != localDeviceDto) {
                deviceCache.saveDevice(remoteDevice, remoteDeviceDto.programId)
            }

            DomainResult.Success(remoteDevice)
        } catch (e: IOException) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncSessionIfNeeded(
        localSession: Session, localSiteId: Int, syncedDeviceId: Int, expectedSpecimens: Int
    ): DomainResult<Session, NetworkError> {
        activeTrace?.step = "sync_session"
        activeTrace?.specimenId = null
        activeTrace?.imageId = null
        return try {
            val localSessionDto = SessionDto(
                sessionId = localSession.remoteId,
                frontendId = localSession.localId,
                hardwareId = localSession.hardwareId,
                collectorTitle = localSession.collectorTitle,
                collectorName = localSession.collectorName,
                collectorLastTrainedOn = localSession.collectorLastTrainedOn,
                collectionDate = localSession.collectionDate,
                collectionMethod = localSession.collectionMethod,
                specimenCondition = localSession.specimenCondition,
                createdAt = localSession.createdAt,
                completedAt = localSession.completedAt,
                submittedAt = localSession.submittedAt,
                notes = localSession.notes,
                latitude = localSession.latitude,
                longitude = localSession.longitude,
                gpsAccuracy = localSession.gpsAccuracy,
                type = localSession.type,
                expectedSpecimens = expectedSpecimens,
                siteId = localSiteId,
                deviceId = syncedDeviceId,
            )

            val remoteSessionDto = when (val remoteSessionResult =
                sessionDataSource.getSessionByFrontendId(localSession.localId)) {
                is DomainResult.Success -> remoteSessionResult.data
                is DomainResult.Error -> {
                    when (remoteSessionResult.error) {
                        NetworkError.NOT_FOUND -> {
                            val postSessionResult = sessionDataSource.postSession(
                                localSession, localSiteId, syncedDeviceId, expectedSpecimens
                            )
                            when (postSessionResult) {
                                is DomainResult.Success -> postSessionResult.data.session
                                is DomainResult.Error -> return DomainResult.Error(postSessionResult.error)
                            }
                        }

                        else -> return DomainResult.Error(remoteSessionResult.error)
                    }
                }
            }

            val remoteSession = Session(
                localId = remoteSessionDto.frontendId,
                remoteId = remoteSessionDto.sessionId,
                hardwareId = remoteSessionDto.hardwareId,
                collectorTitle = remoteSessionDto.collectorTitle,
                collectorName = remoteSessionDto.collectorName,
                collectorLastTrainedOn = remoteSessionDto.collectorLastTrainedOn ?: 0L,
                collectionDate = remoteSessionDto.collectionDate,
                collectionMethod = remoteSessionDto.collectionMethod,
                specimenCondition = remoteSessionDto.specimenCondition,
                createdAt = remoteSessionDto.createdAt,
                completedAt = remoteSessionDto.completedAt,
                submittedAt = remoteSessionDto.submittedAt,
                notes = remoteSessionDto.notes,
                latitude = remoteSessionDto.latitude,
                longitude = remoteSessionDto.longitude,
                gpsAccuracy = remoteSessionDto.gpsAccuracy,
                type = remoteSessionDto.type,
            )

            if (localSessionDto != remoteSessionDto) {
                val updateSessionResult =
                    sessionRepository.upsertSession(remoteSession, remoteSessionDto.siteId)
                if (updateSessionResult is DomainResult.Error) {
                    return DomainResult.Error(NetworkError.CLIENT_ERROR)
                }
            }

            DomainResult.Success(remoteSession)
        } catch (e: IOException) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncSessionUnitIfNeeded(
        localSessionUnit: SessionUnit,
        syncedLocalSessionId: UUID,
        syncedRemoteSessionId: Int,
    ): DomainResult<SessionUnit, NetworkError> {
        activeTrace?.step = "sync_session_unit"
        activeTrace?.specimenId = null
        activeTrace?.imageId = localSessionUnit.localId.toString()
        return try {
            val localSessionUnitDto = SessionUnitDto(
                id = localSessionUnit.remoteId,
                frontendId = localSessionUnit.localId,
                sessionId = syncedRemoteSessionId,
                unitOrder = localSessionUnit.unitOrder,
                createdAt = localSessionUnit.createdAt,
            )

            val remoteSessionUnitsDto = when (val remoteSessionUnitResult =
                sessionUnitDataSource.getSessionUnitForSession(syncedRemoteSessionId)) {
                is DomainResult.Success -> remoteSessionUnitResult.data.units
                is DomainResult.Error -> return DomainResult.Error(remoteSessionUnitResult.error)
            }

            val remoteSessionUnitDto = when (val existingSessionUnit = remoteSessionUnitsDto.firstOrNull { it.frontendId == localSessionUnit.localId }) {
                null -> {
                    when (val postSessionUnitResult = sessionUnitDataSource.postSessionUnit(localSessionUnit, syncedRemoteSessionId)) {
                        is DomainResult.Success -> postSessionUnitResult.data.sessionUnit
                        is DomainResult.Error -> return DomainResult.Error(postSessionUnitResult.error)
                    }
                }
                else -> existingSessionUnit
            }

            val remoteSessionUnit = SessionUnit(
                localId = remoteSessionUnitDto.frontendId,
                remoteId = remoteSessionUnitDto.id,
                unitOrder = remoteSessionUnitDto.unitOrder,
                createdAt = remoteSessionUnitDto.createdAt
            )

            if (localSessionUnitDto != remoteSessionUnitDto) {
                val updateSessionUnitResult = sessionUnitRepository.upsertSessionUnit(
                    remoteSessionUnit, syncedLocalSessionId
                )
                if (updateSessionUnitResult is DomainResult.Error) {
                    return DomainResult.Error(NetworkError.CLIENT_ERROR)
                }
            }

            DomainResult.Success(remoteSessionUnit)
        } catch (e: IOException) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncSurveillanceFormIfNeeded(
        localSurveillanceForm: SurveillanceForm,
        syncedLocalSessionId: UUID,
        syncedRemoteSessionId: Int
    ): DomainResult<Unit, NetworkError> {
        activeTrace?.step = "sync_surveillance_form"
        activeTrace?.specimenId = null
        activeTrace?.imageId = null
        return try {
            val localSurveillanceFormDto = SurveillanceFormDto(
                sessionId = syncedRemoteSessionId,
                numPeopleSleptInHouse = localSurveillanceForm.numPeopleSleptInHouse,
                wasIrsConducted = localSurveillanceForm.wasIrsConducted,
                monthsSinceIrs = localSurveillanceForm.monthsSinceIrs,
                numLlinsAvailable = localSurveillanceForm.numLlinsAvailable,
                llinType = localSurveillanceForm.llinType,
                llinBrand = localSurveillanceForm.llinBrand,
                numPeopleSleptUnderLlin = localSurveillanceForm.numPeopleSleptUnderLlin,
                numChildrenUnder5 = localSurveillanceForm.numChildrenUnder5,
                hasPregnantWoman = localSurveillanceForm.hasPregnantWoman,
                submittedAt = localSurveillanceForm.submittedAt
            )

            val remoteSurveillanceFormDto = when (val remoteSurveillanceFormResult =
                surveillanceFormDataSource.getSurveillanceFormBySessionId(syncedRemoteSessionId)) {
                is DomainResult.Success -> remoteSurveillanceFormResult.data
                is DomainResult.Error -> {
                    when (remoteSurveillanceFormResult.error) {
                        NetworkError.NOT_FOUND -> {
                            val postSurveillanceFormResult =
                                surveillanceFormDataSource.postSurveillanceForm(
                                    localSurveillanceForm, syncedRemoteSessionId
                                )
                            when (postSurveillanceFormResult) {
                                is DomainResult.Success -> postSurveillanceFormResult.data.form
                                is DomainResult.Error -> return DomainResult.Error(
                                    postSurveillanceFormResult.error
                                )
                            }
                        }

                        else -> return DomainResult.Error(remoteSurveillanceFormResult.error)
                    }
                }
            }

            val remoteSurveillanceForm = SurveillanceForm(
                numPeopleSleptInHouse = remoteSurveillanceFormDto.numPeopleSleptInHouse,
                wasIrsConducted = remoteSurveillanceFormDto.wasIrsConducted,
                monthsSinceIrs = remoteSurveillanceFormDto.monthsSinceIrs,
                numLlinsAvailable = remoteSurveillanceFormDto.numLlinsAvailable,
                llinType = remoteSurveillanceFormDto.llinType,
                llinBrand = remoteSurveillanceFormDto.llinBrand,
                numPeopleSleptUnderLlin = remoteSurveillanceFormDto.numPeopleSleptUnderLlin,
                numChildrenUnder5 = remoteSurveillanceFormDto.numChildrenUnder5,
                hasPregnantWoman = remoteSurveillanceFormDto.hasPregnantWoman,
                submittedAt = remoteSurveillanceFormDto.submittedAt
            )

            if (remoteSurveillanceFormDto != localSurveillanceFormDto) {
                val updateSurveillanceFormResult =
                    surveillanceFormRepository.upsertSurveillanceForm(
                        remoteSurveillanceForm, syncedLocalSessionId
                    )
                if (updateSurveillanceFormResult is DomainResult.Error) {
                    return DomainResult.Error(NetworkError.CLIENT_ERROR)
                }
            }

            DomainResult.Success(Unit)
        } catch (e: IOException) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncFormAnswersIfNeeded(
        sessionScopedAnswers: Map<Int, FormAnswer>,
        sessionUnitScopedAnswersBySessionUnitLocalId: Map<UUID, Map<Int, FormAnswer>>,
        syncedSessionUnitsByLocalId: Map<UUID, SessionUnit>,
        syncedLocalSessionId: UUID,
        syncedRemoteSessionId: Int,
    ): DomainResult<Unit, NetworkError> {
        return try {
            val sessionUnitScopedAnswersBySessionUnitRemoteId =
                sessionUnitScopedAnswersBySessionUnitLocalId.mapNotNull { (localId, answers) ->
                    syncedSessionUnitsByLocalId[localId]?.remoteId?.let { it to answers }
                }.toMap()

            val questionIds = (sessionScopedAnswers.keys + sessionUnitScopedAnswersBySessionUnitRemoteId.values.flatMap { it.keys })
                .distinct()
            if (questionIds.isEmpty()) return DomainResult.Success(Unit)

            val formId = questionIds
                .map { formQuestionRepository.getFormIdByQuestionId(it) }
                .distinct()
                .singleOrNull()
                ?: return DomainResult.Error(NetworkError.CLIENT_ERROR)

            val formVersion = formRepository.getFormById(formId)?.version
                ?: return DomainResult.Error(NetworkError.CLIENT_ERROR)

            val localFormAnswersDtoByKey: Map<Pair<Int, Int?>, FormAnswerDto> = buildMap {
                sessionScopedAnswers.forEach { (questionId, answer) ->
                    put(
                        questionId to null, FormAnswerDto(
                            id = answer.remoteId,
                            frontendId = answer.localId,
                            sessionId = syncedRemoteSessionId,
                            sessionUnitId = null,
                            questionId = questionId,
                            value = answer.value,
                            dataType = answer.dataType,
                            submittedAt = answer.submittedAt
                        )
                    )
                }
                sessionUnitScopedAnswersBySessionUnitRemoteId.forEach { (remoteSessionUnitId, answers) ->
                    answers.forEach { (questionId, answer) ->
                        put(
                            questionId to remoteSessionUnitId, FormAnswerDto(
                                id = answer.remoteId,
                                frontendId = answer.localId,
                                sessionId = syncedRemoteSessionId,
                                sessionUnitId = remoteSessionUnitId,
                                questionId = questionId,
                                value = answer.value,
                                dataType = answer.dataType,
                                submittedAt = answer.submittedAt
                            )
                        )
                    }
                }
            }

            val existingRemoteAnswers = when (val getResult =
                formAnswerDataSource.getFormAnswersForSession(syncedRemoteSessionId)) {
                is DomainResult.Success -> getResult.data.answers
                is DomainResult.Error -> when (getResult.error) {
                    NetworkError.NOT_FOUND -> emptyList()
                    else -> return DomainResult.Error(getResult.error)
                }
            }

            val remoteFormAnswersDto = existingRemoteAnswers.ifEmpty {
                when (val postResult = formAnswerDataSource.postFormAnswersForSession(
                    syncedRemoteSessionId,
                    formVersion,
                    sessionScopedAnswers,
                    sessionUnitScopedAnswersBySessionUnitRemoteId,
                )) {
                    is DomainResult.Success -> postResult.data.answers
                    is DomainResult.Error -> return DomainResult.Error(postResult.error)
                }
            }

            for (remoteFormAnswerDto in remoteFormAnswersDto) {
                val localFormAnswerDto =
                    localFormAnswersDtoByKey[remoteFormAnswerDto.questionId to remoteFormAnswerDto.sessionUnitId]
                if (localFormAnswerDto == remoteFormAnswerDto) continue

                val remoteFormAnswer = FormAnswer(
                    localId = remoteFormAnswerDto.frontendId,
                    remoteId = remoteFormAnswerDto.id,
                    value = remoteFormAnswerDto.value,
                    dataType = remoteFormAnswerDto.dataType,
                    submittedAt = remoteFormAnswerDto.submittedAt,
                )
                val localSessionUnitId = remoteFormAnswerDto.sessionUnitId?.let { remoteUnitId ->
                    syncedSessionUnitsByLocalId.entries.firstOrNull { it.value.remoteId == remoteUnitId }?.key
                }
                val formAnswerUpdateResult = formAnswerRepository.upsertFormAnswer(
                    remoteFormAnswer,
                    syncedLocalSessionId,
                    localSessionUnitId,
                    remoteFormAnswerDto.questionId,
                )
                if (formAnswerUpdateResult is DomainResult.Error) {
                    return DomainResult.Error(NetworkError.CLIENT_ERROR)
                }
            }
            DomainResult.Success(Unit)
        } catch (e: IOException) {
            tracedFailure(e, NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            tracedFailure(e, NetworkError.UNKNOWN_ERROR)
        }
    }

    private fun <T> tracedFailure(
        error: Throwable,
        networkError: NetworkError,
    ): DomainResult<T, NetworkError> {
        activeTrace?.failureCause = error
        return DomainResult.Error(networkError)
    }

    private suspend fun syncSpecimenIfNeeded(
        localSpecimen: Specimen,
        syncedLocalSessionId: UUID,
        syncedRemoteSessionId: Int,
        syncedLocalSessionUnitId: UUID?,
        syncedRemoteSessionUnitId: Int?,
        expectedImages: Int,
    ): DomainResult<Specimen, NetworkError> {
        activeTrace?.step = "sync_specimen"
        activeTrace?.specimenId = localSpecimen.id
        activeTrace?.imageId = null
        return try {
            val localSpecimenDto = SpecimenDto(
                id = localSpecimen.remoteId,
                specimenId = localSpecimen.id,
                sessionId = syncedRemoteSessionId,
                sessionUnitId = syncedRemoteSessionUnitId,
                expectedImages = expectedImages,
            )

            val remoteSpecimenDto =
                when (val remoteSpecimenResult = specimenDataSource.getSpecimenByIdAndSessionId(
                    localSpecimen.id, syncedRemoteSessionId
                )) {
                    is DomainResult.Success -> remoteSpecimenResult.data
                    is DomainResult.Error -> {
                        when (remoteSpecimenResult.error) {
                            NetworkError.NOT_FOUND -> {
                                val postSpecimenResult = specimenDataSource.postSpecimen(
                                    localSpecimen,
                                    syncedRemoteSessionId,
                                    syncedRemoteSessionUnitId,
                                    expectedImages,
                                )
                                when (postSpecimenResult) {
                                    is DomainResult.Success -> postSpecimenResult.data.specimen
                                    is DomainResult.Error -> return DomainResult.Error(
                                        postSpecimenResult.error
                                    )
                                }
                            }

                            else -> return DomainResult.Error(remoteSpecimenResult.error)
                        }
                    }
                }

            val remoteSpecimen = Specimen(
                id = remoteSpecimenDto.specimenId,
                remoteId = remoteSpecimenDto.id,
                shouldProcessFurther = remoteSpecimenDto.shouldProcessFurther,
            )

            if (remoteSpecimenDto != localSpecimenDto) {
                val specimenUpdateResult = specimenRepository.updateSpecimen(
                    remoteSpecimen,
                    syncedLocalSessionId,
                    syncedLocalSessionUnitId,
                )
                if (specimenUpdateResult is DomainResult.Error) {
                    return DomainResult.Error(NetworkError.CLIENT_ERROR)
                }
            }

            DomainResult.Success(remoteSpecimen)
        } catch (e: IOException) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncSpecimenImageAndInferenceResultIfNeeded(
        localSpecimenImage: SpecimenImage,
        localInferenceResult: InferenceResult?,
        syncedSpecimen: Specimen,
        syncedLocalSessionId: UUID
    ): DomainResult<Unit, NetworkError> {
        activeTrace?.step = "sync_specimen_image"
        activeTrace?.specimenId = syncedSpecimen.id
        activeTrace?.imageId = localSpecimenImage.localId
        return try {
            specimenImageRepository.updateSpecimenImage(
                localSpecimenImage.copy(metadataUploadStatus = UploadStatus.IN_PROGRESS),
                syncedSpecimen.id,
                syncedLocalSessionId
            )

            val localSpecimenImageDto = SpecimenImageDto(
                id = localSpecimenImage.remoteId,
                filemd5 = localSpecimenImage.localId,
                species = localSpecimenImage.species,
                sex = localSpecimenImage.sex,
                abdomenStatus = localSpecimenImage.abdomenStatus,
                capturedAt = localSpecimenImage.capturedAt,
                submittedAt = localSpecimenImage.submittedAt,
                inferenceResult = localInferenceResult?.let {
                    InferenceResultDto(
                        bboxTopLeftX = it.bboxTopLeftX,
                        bboxTopLeftY = it.bboxTopLeftY,
                        bboxWidth = it.bboxWidth,
                        bboxHeight = it.bboxHeight,
                        bboxConfidence = it.bboxConfidence,
                        bboxClassId = it.bboxClassId,
                        speciesLogits = it.speciesLogits,
                        sexLogits = it.sexLogits,
                        abdomenStatusLogits = it.abdomenStatusLogits,
                        bboxDetectionDuration = it.bboxDetectionDuration,
                        speciesInferenceDuration = it.speciesInferenceDuration,
                        sexInferenceDuration = it.sexInferenceDuration,
                        abdomenStatusInferenceDuration = it.abdomenStatusInferenceDuration
                    )
                },
                imageMetadata = localSpecimenImage.imageMetadata?.toImageMetadataDto()
            )

            if (syncedSpecimen.remoteId == null) {
                return DomainResult.Error(NetworkError.CLIENT_ERROR)
            }

            val remoteSpecimenImageDto = when (val remoteSpecimenImageResult =
                specimenImageDataSource.getSpecimenImageMetadata(
                    localSpecimenImage.localId, syncedSpecimen.remoteId
                )) {
                is DomainResult.Success -> remoteSpecimenImageResult.data
                is DomainResult.Error -> {
                    when (remoteSpecimenImageResult.error) {
                        NetworkError.NOT_FOUND -> {
                            val postSpecimenImageResult =
                                specimenImageDataSource.postSpecimenImageMetadata(
                                    localSpecimenImage,
                                    localInferenceResult,
                                    syncedSpecimen.remoteId
                                )
                            when (postSpecimenImageResult) {
                                is DomainResult.Success -> {
                                    postSpecimenImageResult.data.image
                                }

                                is DomainResult.Error -> return DomainResult.Error(
                                    postSpecimenImageResult.error
                                )
                            }
                        }

                        else -> return DomainResult.Error(remoteSpecimenImageResult.error)
                    }
                }
            }

            val remoteSpecimenImage = SpecimenImage(
                localId = remoteSpecimenImageDto.filemd5,
                remoteId = remoteSpecimenImageDto.id,
                species = remoteSpecimenImageDto.species,
                sex = remoteSpecimenImageDto.sex,
                abdomenStatus = remoteSpecimenImageDto.abdomenStatus,
                imageUri = localSpecimenImage.imageUri,
                metadataUploadStatus = localSpecimenImage.metadataUploadStatus,
                imageUploadStatus = localSpecimenImage.imageUploadStatus,
                capturedAt = remoteSpecimenImageDto.capturedAt,
                submittedAt = remoteSpecimenImageDto.submittedAt,
                imageMetadata = remoteSpecimenImageDto.imageMetadata?.toCameraMetadata()
                    ?: localSpecimenImage.imageMetadata
            )

            val remoteInferenceResult = remoteSpecimenImageDto.inferenceResult?.let {
                InferenceResult(
                    bboxTopLeftX = it.bboxTopLeftX,
                    bboxTopLeftY = it.bboxTopLeftY,
                    bboxWidth = it.bboxWidth,
                    bboxHeight = it.bboxHeight,
                    bboxConfidence = it.bboxConfidence,
                    bboxClassId = it.bboxClassId,
                    speciesLogits = it.speciesLogits,
                    sexLogits = it.sexLogits,
                    abdomenStatusLogits = it.abdomenStatusLogits,
                    bboxDetectionDuration = it.bboxDetectionDuration,
                    speciesInferenceDuration = it.speciesInferenceDuration,
                    sexInferenceDuration = it.sexInferenceDuration,
                    abdomenStatusInferenceDuration = it.abdomenStatusInferenceDuration,
                )
            }

            if (remoteSpecimenImageDto != localSpecimenImageDto) {
                val transactionResult = transactionHelper.runAsTransaction {
                    specimenImageRepository.updateSpecimenImage(
                        remoteSpecimenImage, syncedSpecimen.id, syncedLocalSessionId
                    ).onError {
                        return@runAsTransaction DomainResult.Error(NetworkError.CLIENT_ERROR)
                    }

                    remoteInferenceResult?.let { remoteInferenceResult ->
                        inferenceResultRepository.updateInferenceResult(
                            remoteInferenceResult, remoteSpecimenImage.localId
                        ).onError {
                            return@runAsTransaction DomainResult.Error(NetworkError.CLIENT_ERROR)
                        }
                    }

                    DomainResult.Success(Unit)
                }

                if (transactionResult is DomainResult.Error) {
                    return DomainResult.Error(transactionResult.error)
                }
            }

            specimenImageRepository.updateSpecimenImage(
                remoteSpecimenImage.copy(metadataUploadStatus = UploadStatus.COMPLETED),
                syncedSpecimen.id,
                syncedLocalSessionId
            )
            DomainResult.Success(Unit)
        } catch (e: IOException) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            activeTrace?.failureCause = e
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.upload_notification_channel_metadata),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun showInitialMetadataNotification(): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.upload_notification_registering))
            .setSmallIcon(R.drawable.ic_cloud_upload).setOngoing(true).build()

        return ForegroundInfo(
            notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun showSpecimenProgressNotification(
        specimenId: String,
        currentSpecimenIndex: Int,
        totalSpecimens: Int,
        currentImageIndex: Int,
        totalImagesForSpecimen: Int
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.upload_notification_uploading_metadata, specimenId))
            .setContentText(context.getString(R.string.upload_notification_specimen_progress, currentSpecimenIndex + 1, totalSpecimens)).setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.upload_notification_specimen_image_progress, currentSpecimenIndex + 1, totalSpecimens, currentImageIndex + 1, totalImagesForSpecimen)
                )
            ).setSmallIcon(R.drawable.ic_cloud_upload)
            .setProgress(totalSpecimens, currentSpecimenIndex + 1, false).setOngoing(true).build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showUploadRetryNotification(message: String) {
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID).setContentTitle(context.getString(R.string.upload_notification_retrying))
                .setContentText(message).setSmallIcon(R.drawable.ic_error).build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showUploadErrorNotification(message: String) {
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID).setContentTitle(context.getString(R.string.upload_notification_failed))
                .setContentText(message).setSmallIcon(R.drawable.ic_error).build()

        notificationManager.notify(notificationId, notification)
    }
}
