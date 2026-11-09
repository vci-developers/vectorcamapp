package com.vci.vectorcamapp.core.data.upload.metadata

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.data.dto.data_recovery.DataRecoveryPayloadDto
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryRequestDto
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
import com.vci.vectorcamapp.core.data.mappers.toPostFormAnswersRequestDto
import com.vci.vectorcamapp.core.data.mappers.toPostSessionRequestDto
import com.vci.vectorcamapp.core.data.mappers.toPostSessionUnitRequestDto
import com.vci.vectorcamapp.core.data.mappers.toPostSpecimenImageRequestDto
import com.vci.vectorcamapp.core.data.mappers.toPostSpecimenRequestDto
import com.vci.vectorcamapp.core.data.mappers.toPostSurveillanceFormRequestDto
import com.vci.vectorcamapp.core.data.mappers.toRegisterDeviceRequestDto
import com.vci.vectorcamapp.core.data.network.api.UploadRoutes
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.DataRecoverySubmission
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.network.api.DataRecoveryDataSource
import com.vci.vectorcamapp.core.domain.network.api.DeviceDataSource
import com.vci.vectorcamapp.core.domain.network.api.FormAnswerDataSource
import com.vci.vectorcamapp.core.domain.network.api.SessionDataSource
import com.vci.vectorcamapp.core.domain.network.api.SessionUnitDataSource
import com.vci.vectorcamapp.core.domain.network.api.SpecimenDataSource
import com.vci.vectorcamapp.core.domain.network.api.SpecimenImageDataSource
import com.vci.vectorcamapp.core.domain.network.api.SurveillanceFormDataSource
import com.vci.vectorcamapp.core.domain.repository.DataRecoverySubmissionRepository
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.repository.InferenceResultRepository
import com.vci.vectorcamapp.core.domain.repository.MetadataUploadStatusRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.SurveillanceFormRepository
import com.vci.vectorcamapp.core.domain.util.errorOrNull
import com.vci.vectorcamapp.core.domain.util.map
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.core.domain.util.successOrNull
import com.vci.vectorcamapp.core.presentation.util.error.toString
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import timber.log.Timber
import java.util.Calendar
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
    private val dataRecoveryDataSource: DataRecoveryDataSource,
    private val dataRecoverySubmissionRepository: DataRecoverySubmissionRepository,
    private val formAnswerRepository: FormAnswerRepository,
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository,
    private val metadataUploadStatusRepository: MetadataUploadStatusRepository,
    private val notifier: MetadataUploadNotifier,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val MAX_RETRIES = 5
        const val MAX_API_RETRIES = 3
        const val API_RETRY_BASE_DELAY_MS = 1000L
        private const val TAG = "DataRecovery"

        private val recoveryJson = Json {
            encodeDefaults = true
            explicitNulls = false
        }
    }

    private var notificationId = 1001
    private val uploadOutcomes = MetadataUploadOutcomes()
    private val submittedRecoveryKeys = mutableSetOf<String>()

    override suspend fun doWork(): WorkerResult {
        val localSessionIdString = inputData.getString("session_id")
        val localSiteId = inputData.getInt("site_id", -1)
        if (localSessionIdString == null || localSiteId == -1) {
            return retryOrFailure(context.getString(R.string.upload_error_invalid_request))
        }

        val localSessionId = try {
            UUID.fromString(localSessionIdString)
        } catch (e: IllegalArgumentException) {
            return retryOrFailure(context.getString(R.string.upload_error_invalid_session_id))
        }

        val localSession = sessionRepository.getSessionById(localSessionId)
        val localDevice = deviceCache.getDevice()
        val localProgramId = deviceCache.getProgramId()
        if (localSession == null || localDevice == null || localProgramId == null) {
            return retryOrFailure(context.getString(R.string.upload_error_device_session_not_found))
        }

        val localProgram = programRepository.getProgramById(localProgramId)
            ?: return retryOrFailure(context.getString(R.string.upload_error_program_not_found))

        notifier.createNotificationChannel()
        notificationId = localSessionId.hashCode()
        setForeground(notifier.buildInitialMetadataForegroundInfo(notificationId))

        val collectionCalendar = Calendar.getInstance().apply { timeInMillis = localSession.collectionDate }
        val recoveryTemplate = PostDataRecoveryRequestDto(
            programId = localProgramId,
            siteId = localSiteId,
            sessionFrontendId = localSessionId,
            year = collectionCalendar.get(Calendar.YEAR),
            month = collectionCalendar.get(Calendar.MONTH) + 1,
            payload = DataRecoveryPayloadDto(
                userId = "${localDevice.id}_${localDevice.registeredAt}",
                deviceId = localDevice.id,
                method = "POST",
                workerAttempt = runAttemptCount + 1,
            ),
        )

        try {
            submittedRecoveryKeys += dataRecoverySubmissionRepository.getSubmittedIdempotencyKeysForSession(
                localSessionId
            )
            val dependencyFailureMessage = context.getString(R.string.upload_error_dependency_not_uploaded)

            val usesDynamicForm = localProgram.formVersion != null
            val localSessionUnits = if (usesDynamicForm) {
                sessionUnitRepository.getSessionUnitsForSession(localSessionId)
            } else {
                emptyList()
            }
            val sessionScopedFormAnswers = if (usesDynamicForm) {
                formAnswerRepository.getSessionScopedFormAnswers(localSessionId)
            } else {
                emptyMap()
            }
            val sessionUnitScopedFormAnswersByLocalUnitId = localSessionUnits.associate { localSessionUnit ->
                localSessionUnit.localId to formAnswerRepository.getSessionUnitScopedFormAnswers(localSessionUnit.localId)
            }.filterValues { it.isNotEmpty() }
            val hasFormAnswers =
                sessionScopedFormAnswers.isNotEmpty() || sessionUnitScopedFormAnswersByLocalUnitId.isNotEmpty()
            val localSurveillanceForm = if (usesDynamicForm) {
                null
            } else {
                surveillanceFormRepository.getSurveillanceFormBySessionId(localSessionId)
            }
            val localSpecimensWithImagesAndInferenceResults =
                specimenRepository.getSpecimenImagesAndInferenceResultsBySessionScope(
                    localSessionId, null
                )
            val totalSpecimensForSession = localSpecimensWithImagesAndInferenceResults.size
            val hasSpecimenImages = localSpecimensWithImagesAndInferenceResults.any {
                it.specimenImagesAndInferenceResults.isNotEmpty()
            }

            val applicableSteps = buildList {
                add(MetadataUploadStep.DEVICE)
                add(MetadataUploadStep.SESSION)
                if (localSessionUnits.isNotEmpty()) add(MetadataUploadStep.SESSION_UNITS)
                if (hasFormAnswers) add(MetadataUploadStep.FORM_ANSWERS)
                if (localSurveillanceForm != null) add(MetadataUploadStep.SURVEILLANCE_FORM)
                if (totalSpecimensForSession > 0) add(MetadataUploadStep.SPECIMENS)
                if (hasSpecimenImages) add(MetadataUploadStep.SPECIMEN_IMAGES)
            }
            markStepsInProgress(localSessionId, applicableSteps)

            val syncedDevice = syncWithRecovery(
                recoveryTemplate = recoveryTemplate,
                step = MetadataUploadStep.DEVICE,
                itemId = localDevice.id.toString(),
                route = UploadRoutes.REGISTER_DEVICE,
                requestData = {
                    recoveryJson.encodeToJsonElement(localDevice.toRegisterDeviceRequestDto(localProgramId))
                },
            ) { syncDeviceIfNeeded(localDevice, localProgramId) }.successOrNull() ?: localDevice
            recordStepStatus(localSessionId, MetadataUploadStep.DEVICE)

            val syncSessionResult = syncWithRecovery(
                recoveryTemplate = recoveryTemplate,
                step = MetadataUploadStep.SESSION,
                itemId = localSessionId.toString(),
                route = UploadRoutes.SESSIONS,
                requestData = {
                    recoveryJson.encodeToJsonElement(
                        localSession.toPostSessionRequestDto(
                            localSiteId,
                            syncedDevice.id,
                            totalSpecimensForSession
                        )
                    )
                },
            ) {
                syncSessionIfNeeded(
                    localSession,
                    localSiteId,
                    syncedDevice.id,
                    totalSpecimensForSession
                )
            }
            val syncedSession = syncSessionResult.successOrNull()
            val syncedRemoteSessionId = syncedSession?.remoteId
            if (syncedSession == null || syncedRemoteSessionId == null) {
                if (syncSessionResult is DomainResult.Success) {
                    uploadOutcomes.markFailed(
                        MetadataUploadStep.SESSION,
                        context.getString(R.string.upload_error_session_not_found_server)
                    )
                }
                recordStepStatus(localSessionId, MetadataUploadStep.SESSION)
                // Every remaining endpoint is scoped under the remote session id.
                failInProgressSteps(localSessionId)
                return retryOrFailure(
                    syncSessionResult.errorOrNull()?.toString(context)
                        ?: context.getString(R.string.upload_error_session_not_found_server)
                )
            }
            recordStepStatus(localSessionId, MetadataUploadStep.SESSION)

            val syncedSessionUnitsByLocalId = mutableMapOf<UUID, SessionUnit>()
            val failedSessionUnitLocalIds = mutableSetOf<UUID>()
            localSessionUnits.forEach { localSessionUnit ->
                syncWithRecovery(
                    recoveryTemplate = recoveryTemplate,
                    step = MetadataUploadStep.SESSION_UNITS,
                    itemId = localSessionUnit.localId.toString(),
                    route = UploadRoutes.sessionUnits(syncedRemoteSessionId),
                    requestData = {
                        recoveryJson.encodeToJsonElement(localSessionUnit.toPostSessionUnitRequestDto())
                    },
                ) {
                    syncSessionUnitIfNeeded(
                        localSessionUnit,
                        syncedSession.localId,
                        syncedRemoteSessionId
                    )
                }.onSuccess { syncedSessionUnit ->
                    syncedSessionUnitsByLocalId[localSessionUnit.localId] = syncedSessionUnit
                }.onError {
                    failedSessionUnitLocalIds += localSessionUnit.localId
                }
            }
            if (localSessionUnits.isNotEmpty()) {
                recordStepStatus(localSessionId, MetadataUploadStep.SESSION_UNITS)
            }

            if (hasFormAnswers) {
                // The answers endpoint only accepts the first submission for a session, so posting before
                // every unit has a remote id would permanently drop the answers of the missing units.
                if (failedSessionUnitLocalIds.isNotEmpty()) {
                    uploadOutcomes.markFailed(MetadataUploadStep.FORM_ANSWERS, dependencyFailureMessage)
                } else {
                    val sessionUnitScopedFormAnswersByRemoteUnitId =
                        sessionUnitScopedFormAnswersByLocalUnitId.mapNotNull { (localUnitId, answers) ->
                            syncedSessionUnitsByLocalId[localUnitId]?.remoteId?.let { it to answers }
                        }.toMap()

                    syncWithRecovery(
                        recoveryTemplate = recoveryTemplate,
                        step = MetadataUploadStep.FORM_ANSWERS,
                        itemId = localSessionId.toString(),
                        route = UploadRoutes.formAnswers(syncedRemoteSessionId),
                        requestData = {
                            recoveryJson.encodeToJsonElement(
                                toPostFormAnswersRequestDto(
                                    formVersion = resolveFormVersion(
                                        sessionScopedFormAnswers, sessionUnitScopedFormAnswersByRemoteUnitId
                                    ) ?: localProgram.formVersion.orEmpty(),
                                    sessionScopedAnswers = sessionScopedFormAnswers,
                                    sessionUnitScopedAnswersBySessionUnitRemoteId = sessionUnitScopedFormAnswersByRemoteUnitId,
                                    submittedAt = System.currentTimeMillis(),
                                )
                            )
                        },
                    ) {
                        syncFormAnswersIfNeeded(
                            sessionScopedFormAnswers,
                            sessionUnitScopedFormAnswersByRemoteUnitId,
                            syncedSessionUnitsByLocalId,
                            syncedSession.localId,
                            syncedRemoteSessionId,
                        )
                    }
                }
                recordStepStatus(localSessionId, MetadataUploadStep.FORM_ANSWERS)
            }

            if (localSurveillanceForm != null) {
                syncWithRecovery(
                    recoveryTemplate = recoveryTemplate,
                    step = MetadataUploadStep.SURVEILLANCE_FORM,
                    itemId = localSessionId.toString(),
                    route = UploadRoutes.surveillanceForm(syncedRemoteSessionId),
                    requestData = {
                        recoveryJson.encodeToJsonElement(
                            localSurveillanceForm.toPostSurveillanceFormRequestDto(syncedRemoteSessionId)
                        )
                    },
                ) {
                    syncSurveillanceFormIfNeeded(
                        localSurveillanceForm,
                        syncedSession.localId,
                        syncedRemoteSessionId
                    )
                }
                recordStepStatus(localSessionId, MetadataUploadStep.SURVEILLANCE_FORM)
            }

            localSpecimensWithImagesAndInferenceResults.forEachIndexed { specimenIndex, specimenWithImagesAndInferenceResults ->
                val localSpecimen = specimenWithImagesAndInferenceResults.specimen
                val totalImagesForSpecimen =
                    specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.size

                val syncedLocalSessionUnitId = specimenRepository.getSessionUnitIdForSpecimen(
                    localSpecimen.id,
                    syncedSession.localId,
                )
                val syncedRemoteSessionUnitId = syncedLocalSessionUnitId?.let {
                    syncedSessionUnitsByLocalId[it]?.remoteId
                }

                // A specimen whose unit failed to sync would otherwise be created without its unit link.
                val syncedSpecimen = if (syncedLocalSessionUnitId in failedSessionUnitLocalIds) {
                    uploadOutcomes.markFailed(MetadataUploadStep.SPECIMENS, dependencyFailureMessage)
                    null
                } else {
                    syncWithRecovery(
                        recoveryTemplate = recoveryTemplate,
                        step = MetadataUploadStep.SPECIMENS,
                        itemId = localSpecimen.id,
                        route = UploadRoutes.specimens(syncedRemoteSessionId),
                        requestData = {
                            recoveryJson.encodeToJsonElement(
                                localSpecimen.toPostSpecimenRequestDto(
                                    syncedRemoteSessionUnitId,
                                    totalImagesForSpecimen
                                )
                            )
                        },
                    ) {
                        syncSpecimenIfNeeded(
                            localSpecimen,
                            syncedSession.localId,
                            syncedRemoteSessionId,
                            syncedLocalSessionUnitId,
                            syncedRemoteSessionUnitId,
                            totalImagesForSpecimen,
                        )
                    }.successOrNull()
                }
                val syncedRemoteSpecimenId = syncedSpecimen?.remoteId

                if (syncedSpecimen == null || syncedRemoteSpecimenId == null) {
                    specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.forEach { (specimenImage, _) ->
                        if (specimenImage.metadataUploadStatus != UploadStatus.COMPLETED) {
                            uploadOutcomes.markFailed(MetadataUploadStep.SPECIMEN_IMAGES, dependencyFailureMessage)
                            specimenImageRepository.updateSpecimenImage(
                                specimenImage.copy(metadataUploadStatus = UploadStatus.FAILED),
                                localSpecimen.id,
                                syncedSession.localId
                            )
                        }
                    }
                    return@forEachIndexed
                }

                specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.forEachIndexed { imageIndex, (specimenImage, inferenceResult) ->
                    if (specimenImage.metadataUploadStatus != UploadStatus.COMPLETED) {
                        syncWithRecovery(
                            recoveryTemplate = recoveryTemplate,
                            step = MetadataUploadStep.SPECIMEN_IMAGES,
                            itemId = specimenImage.localId,
                            route = UploadRoutes.specimenImagesData(syncedRemoteSpecimenId),
                            requestData = {
                                recoveryJson.encodeToJsonElement(
                                    specimenImage.toPostSpecimenImageRequestDto(inferenceResult)
                                )
                            },
                        ) {
                            syncSpecimenImageAndInferenceResultIfNeeded(
                                specimenImage, inferenceResult, syncedSpecimen, syncedSession.localId
                            )
                        }.onSuccess {
                            notifier.showSpecimenProgressNotification(
                                notificationId = notificationId,
                                specimenId = syncedSpecimen.id,
                                currentSpecimenIndex = specimenIndex,
                                totalSpecimens = totalSpecimensForSession,
                                currentImageIndex = imageIndex,
                                totalImagesForSpecimen = totalImagesForSpecimen
                            )
                        }.onError {
                            specimenImageRepository.updateSpecimenImage(
                                specimenImage.copy(metadataUploadStatus = UploadStatus.FAILED),
                                syncedSpecimen.id,
                                syncedSession.localId
                            )
                        }
                    }
                }
            }
            if (totalSpecimensForSession > 0) {
                recordStepStatus(localSessionId, MetadataUploadStep.SPECIMENS)
            }
            if (hasSpecimenImages) {
                recordStepStatus(localSessionId, MetadataUploadStep.SPECIMEN_IMAGES)
            }

            // Failures that were captured by data recovery are not retried here; only data that
            // reached neither the server nor the recovery table keeps the upload pending.
            return uploadOutcomes.firstFailureMessage?.let { retryOrComplete(it) } ?: WorkerResult.success()
        } catch (e: IOException) {
            failInProgressSteps(localSessionId)
            return retryOrFailure(context.getString(R.string.upload_error_lost_connection))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failInProgressSteps(localSessionId)
            return retryOrFailure(context.getString(R.string.upload_error_unknown))
        } finally {
            if (isStopped) {
                withContext(NonCancellable) {
                    resetInProgressUploads(localSession.localId)
                    failInProgressSteps(localSessionId)
                }
            }
        }
    }

    private suspend fun <T> withApiRetries(
        call: suspend () -> DomainResult<T, NetworkError>
    ): DomainResult<T, NetworkError> {
        var result = call()
        for (retry in 1..MAX_API_RETRIES) {
            if (result is DomainResult.Success) break
            delay(API_RETRY_BASE_DELAY_MS * retry)
            result = call()
        }
        return result
    }

    private suspend fun <T> syncWithRecovery(
        recoveryTemplate: PostDataRecoveryRequestDto,
        step: MetadataUploadStep,
        itemId: String,
        route: String,
        requestData: suspend () -> JsonElement,
        sync: suspend () -> DomainResult<T, NetworkError>,
    ): DomainResult<T, NetworkError> {
        val result = withApiRetries(sync)
        if (result is DomainResult.Success) {
            Timber.tag(TAG).i("Upload step $step succeeded for item $itemId on $route. Not sending to recovery.")
        } else if (result is DomainResult.Error) {
            Timber.tag(TAG).e(
                "Upload step $step failed for item $itemId on $route: ${result.error}. " +
                    "Sending this item to POST /data-recoveries."
            )
            val isRecovered = reportToDataRecovery(
                recoveryTemplate = recoveryTemplate,
                idempotencyKey = "${recoveryTemplate.sessionFrontendId}:${step.name}:$itemId",
                step = step,
                route = route,
                error = result.error,
                requestData = requestData,
            )
            if (isRecovered) {
                uploadOutcomes.markRecovered(step)
            } else {
                uploadOutcomes.markFailed(step, result.error.toString(context))
            }
        }
        return result
    }

    private suspend fun reportToDataRecovery(
        recoveryTemplate: PostDataRecoveryRequestDto,
        idempotencyKey: String,
        step: MetadataUploadStep,
        route: String,
        error: NetworkError,
        requestData: suspend () -> JsonElement,
    ): Boolean {
        if (idempotencyKey in submittedRecoveryKeys) {
            Timber.tag(TAG).i("Skipping data recovery POST; already submitted key=$idempotencyKey")
            return true
        }

        val attemptCount = MAX_API_RETRIES + 1
        val recoveryRequest = recoveryTemplate.copy(
            idempotencyKey = idempotencyKey,
            payload = recoveryTemplate.payload.copy(
                route = route,
                error = error.name,
                attemptCount = attemptCount,
                failedAt = System.currentTimeMillis(),
                requestData = requestData(),
            ),
            notes = "${recoveryTemplate.payload.method} $route failed with ${error.name} after $attemptCount attempts",
        )
        Timber.tag(TAG).i(
            "POST /data-recoveries key=$idempotencyKey step=$step route=$route error=${error.name} " +
                "notes=${recoveryRequest.notes}"
        )
        return when (val recoveryResult = withApiRetries { dataRecoveryDataSource.postDataRecovery(recoveryRequest) }) {
            is DomainResult.Success -> {
                Timber.tag(TAG).i(
                    "POST /data-recoveries succeeded key=$idempotencyKey " +
                        "recoveryId=${recoveryResult.data.dataRecovery?.id} " +
                        "duplicate=${recoveryResult.data.duplicate}"
                )
                submittedRecoveryKeys += idempotencyKey
                dataRecoverySubmissionRepository.recordDataRecoverySubmission(
                    DataRecoverySubmission(
                        idempotencyKey = idempotencyKey,
                        step = step,
                        dataRecoveryId = recoveryResult.data.dataRecovery?.id,
                        submittedAt = System.currentTimeMillis(),
                    ),
                    recoveryTemplate.sessionFrontendId,
                ).onError {
                    Timber.tag(TAG).e("Failed to remember data recovery submission $idempotencyKey: $it")
                }
                true
            }

            is DomainResult.Error -> {
                Timber.tag(TAG).e(
                    "POST /data-recoveries failed key=$idempotencyKey route=$route: ${recoveryResult.error}"
                )
                false
            }
        }
    }

    private suspend fun markStepsInProgress(sessionId: UUID, steps: List<MetadataUploadStep>) {
        steps.forEach { step -> upsertStepStatus(sessionId, step, UploadStatus.IN_PROGRESS) }
    }

    private suspend fun recordStepStatus(sessionId: UUID, step: MetadataUploadStep) {
        upsertStepStatus(sessionId, step, uploadOutcomes.statusOf(step))
    }

    private suspend fun upsertStepStatus(
        sessionId: UUID, step: MetadataUploadStep, status: UploadStatus
    ) {
        metadataUploadStatusRepository.upsertMetadataUploadStatus(
            MetadataUploadStepStatus(
                step = step,
                status = status,
                updatedAt = System.currentTimeMillis()
            ),
            sessionId
        ).onError {
            Timber.tag(TAG).e("Failed to record $step upload status for session $sessionId: $it")
        }
    }

    private suspend fun failInProgressSteps(sessionId: UUID) {
        metadataUploadStatusRepository.failInProgressMetadataUploadStatuses(sessionId).onError {
            Timber.tag(TAG).e("Failed to clear in-progress upload statuses for session $sessionId: $it")
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

    private fun retryOrComplete(message: String): WorkerResult {
        notifier.showUploadRetryNotification(notificationId, message)
        return if (runAttemptCount < MAX_RETRIES) {
            WorkerResult.retry()
        } else {
            WorkerResult.success()
        }
    }

    private fun retryOrFailure(message: String): WorkerResult {
        if (runAttemptCount < MAX_RETRIES) {
            notifier.showUploadRetryNotification(notificationId, message)
            return WorkerResult.retry()
        } else {
            notifier.showUploadErrorNotification(notificationId, message)
            return WorkerResult.failure()
        }
    }

    private suspend fun syncDeviceIfNeeded(
        localDevice: Device, localProgramId: Int
    ): DomainResult<Device, NetworkError> {
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
                    is DomainResult.Success -> remoteDeviceResult.data
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

            if (remoteDeviceDto != localDeviceDto) {
                deviceCache.saveDevice(remoteDevice, remoteDeviceDto.programId)
            }

            DomainResult.Success(remoteDevice)
        } catch (e: IOException) {
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncSessionIfNeeded(
        localSession: Session, localSiteId: Int, syncedDeviceId: Int, expectedSpecimens: Int
    ): DomainResult<Session, NetworkError> {
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
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncSessionUnitIfNeeded(
        localSessionUnit: SessionUnit,
        syncedLocalSessionId: UUID,
        syncedRemoteSessionId: Int,
    ): DomainResult<SessionUnit, NetworkError> {
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
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncSurveillanceFormIfNeeded(
        localSurveillanceForm: SurveillanceForm,
        syncedLocalSessionId: UUID,
        syncedRemoteSessionId: Int
    ): DomainResult<Unit, NetworkError> {
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
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun resolveFormVersion(
        sessionScopedAnswers: Map<Int, FormAnswer>,
        sessionUnitScopedAnswersBySessionUnitRemoteId: Map<Int, Map<Int, FormAnswer>>,
    ): String? {
        val formId = (sessionScopedAnswers.keys + sessionUnitScopedAnswersBySessionUnitRemoteId.values.flatMap { it.keys })
            .distinct()
            .map { formQuestionRepository.getFormIdByQuestionId(it) }
            .distinct()
            .singleOrNull()
            ?: return null
        return formRepository.getFormById(formId)?.version
    }

    private suspend fun getOrPostFormAnswers(
        syncedRemoteSessionId: Int,
        formVersion: String,
        sessionScopedAnswers: Map<Int, FormAnswer>,
        sessionUnitScopedAnswersBySessionUnitRemoteId: Map<Int, Map<Int, FormAnswer>>,
    ): DomainResult<List<FormAnswerDto>, NetworkError> {
        val getResult = formAnswerDataSource.getFormAnswersForSession(syncedRemoteSessionId)
        val existingRemoteAnswers = when (getResult) {
            is DomainResult.Success -> getResult.data.answers
            is DomainResult.Error -> when (getResult.error) {
                NetworkError.NOT_FOUND -> emptyList()
                else -> return DomainResult.Error(getResult.error)
            }
        }
        if (existingRemoteAnswers.isNotEmpty()) return DomainResult.Success(existingRemoteAnswers)

        return formAnswerDataSource.postFormAnswersForSession(
            syncedRemoteSessionId,
            formVersion,
            sessionScopedAnswers,
            sessionUnitScopedAnswersBySessionUnitRemoteId,
        ).map { it.answers }
    }

    private suspend fun syncFormAnswersIfNeeded(
        sessionScopedAnswers: Map<Int, FormAnswer>,
        sessionUnitScopedAnswersBySessionUnitRemoteId: Map<Int, Map<Int, FormAnswer>>,
        syncedSessionUnitsByLocalId: Map<UUID, SessionUnit>,
        syncedLocalSessionId: UUID,
        syncedRemoteSessionId: Int,
    ): DomainResult<Unit, NetworkError> {
        if (sessionScopedAnswers.isEmpty() && sessionUnitScopedAnswersBySessionUnitRemoteId.values.all { it.isEmpty() }) {
            return DomainResult.Success(Unit)
        }

        return try {
            val formVersion = resolveFormVersion(
                sessionScopedAnswers, sessionUnitScopedAnswersBySessionUnitRemoteId
            ) ?: return DomainResult.Error(NetworkError.CLIENT_ERROR)

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

            val remoteFormAnswersResult = getOrPostFormAnswers(
                syncedRemoteSessionId,
                formVersion,
                sessionScopedAnswers,
                sessionUnitScopedAnswersBySessionUnitRemoteId,
            )
            val remoteFormAnswersDto = when (remoteFormAnswersResult) {
                is DomainResult.Success -> remoteFormAnswersResult.data
                is DomainResult.Error -> return remoteFormAnswersResult
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
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncSpecimenIfNeeded(
        localSpecimen: Specimen,
        syncedLocalSessionId: UUID,
        syncedRemoteSessionId: Int,
        syncedLocalSessionUnitId: UUID?,
        syncedRemoteSessionUnitId: Int?,
        expectedImages: Int,
    ): DomainResult<Specimen, NetworkError> {
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
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun syncSpecimenImageAndInferenceResultIfNeeded(
        localSpecimenImage: SpecimenImage,
        localInferenceResult: InferenceResult?,
        syncedSpecimen: Specimen,
        syncedLocalSessionId: UUID
    ): DomainResult<Unit, NetworkError> {
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
            DomainResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }
    }
}

private class MetadataUploadOutcomes {
    private val failedSteps = mutableSetOf<MetadataUploadStep>()
    private val recoveredSteps = mutableSetOf<MetadataUploadStep>()

    var firstFailureMessage: String? = null
        private set

    fun markFailed(step: MetadataUploadStep, message: String) {
        failedSteps += step
        if (firstFailureMessage == null) firstFailureMessage = message
    }

    fun markRecovered(step: MetadataUploadStep) {
        recoveredSteps += step
    }

    fun statusOf(step: MetadataUploadStep): UploadStatus = when (step) {
        in failedSteps -> UploadStatus.FAILED
        in recoveredSteps -> UploadStatus.RECOVERED
        else -> UploadStatus.COMPLETED
    }
}
