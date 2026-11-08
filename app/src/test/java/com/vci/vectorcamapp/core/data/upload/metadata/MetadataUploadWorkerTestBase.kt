package com.vci.vectorcamapp.core.data.upload.metadata

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import com.vci.vectorcamapp.core.data.dto.data_recovery.DataRecoveryDto
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryRequestDto
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryResponseDto
import com.vci.vectorcamapp.core.data.dto.device.DeviceDto
import com.vci.vectorcamapp.core.data.dto.form_answer.PostFormAnswersResponseDto
import com.vci.vectorcamapp.core.data.dto.session.PostSessionResponseDto
import com.vci.vectorcamapp.core.data.dto.session.SessionDto
import com.vci.vectorcamapp.core.data.dto.specimen.PostSpecimenResponseDto
import com.vci.vectorcamapp.core.data.dto.specimen.SpecimenDto
import com.vci.vectorcamapp.core.data.dto.specimen_image.PostSpecimenImageResponseDto
import com.vci.vectorcamapp.core.data.dto.specimen_image.SpecimenImageDto
import com.vci.vectorcamapp.core.data.dto.surveillance_form.PostSurveillanceFormResponseDto
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.DataRecoverySubmission
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenImageAndInferenceResult
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
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
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import java.util.UUID

abstract class MetadataUploadWorkerTestBase {

    companion object {
        const val DEVICE_ID = 42
        const val DEVICE_REGISTERED_AT = 1_000L
        const val PROGRAM_ID = 3
        const val SITE_ID = 7
        const val REMOTE_SESSION_ID = 10
        const val FORM_ID = 5
        const val FORM_VERSION = "v1"
        const val QUESTION_ID = 101
        const val SPECIMEN_1 = "S1"
        const val SPECIMEN_2 = "S2"
        const val IMAGE_1 = "md5-1"
        const val IMAGE_2 = "md5-2"
        const val TOTAL_ATTEMPTS_PER_CALL = MetadataUploadWorker.MAX_API_RETRIES + 1
        const val DATA_RECOVERY_ID = 900
        const val COLLECTION_DATE = 1_773_576_000_000L
        const val COLLECTION_YEAR = 2026
        const val COLLECTION_MONTH = 3
    }

    protected val sessionId: UUID = UUID.randomUUID()
    protected val remoteSpecimenIds = mapOf(SPECIMEN_1 to 100, SPECIMEN_2 to 200)

    protected val device = Device(
        id = DEVICE_ID,
        model = "Pixel",
        registeredAt = DEVICE_REGISTERED_AT,
        submittedAt = null
    )
    protected val session = Session(
        localId = sessionId,
        remoteId = null,
        hardwareId = "HW001",
        collectorTitle = "Dr.",
        collectorName = "Alice",
        collectorLastTrainedOn = 0L,
        collectionDate = COLLECTION_DATE,
        collectionMethod = "Net",
        specimenCondition = "Good",
        createdAt = 1_000_100L,
        completedAt = 1_000_200L,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        gpsAccuracy = null,
        type = SessionType.SURVEILLANCE,
    )
    protected val formAnswer = FormAnswer(
        localId = UUID.randomUUID(),
        remoteId = null,
        value = FormAnswerValue.StringValue("yes"),
        dataType = "string",
        submittedAt = 1_000_300L,
    )
    protected val surveillanceForm = SurveillanceForm(
        numPeopleSleptInHouse = 4,
        wasIrsConducted = false,
        monthsSinceIrs = null,
        numLlinsAvailable = 2,
        llinType = null,
        llinBrand = null,
        numPeopleSleptUnderLlin = null,
        numChildrenUnder5 = null,
        hasPregnantWoman = null,
        submittedAt = null,
    )

    protected lateinit var context: Context
    protected lateinit var workerParams: WorkerParameters
    protected lateinit var deviceCache: DeviceCache
    protected lateinit var programRepository: ProgramRepository
    protected lateinit var sessionRepository: SessionRepository
    protected lateinit var sessionUnitRepository: SessionUnitRepository
    protected lateinit var surveillanceFormRepository: SurveillanceFormRepository
    protected lateinit var specimenRepository: SpecimenRepository
    protected lateinit var specimenImageRepository: SpecimenImageRepository
    protected lateinit var inferenceResultRepository: InferenceResultRepository
    protected lateinit var deviceDataSource: DeviceDataSource
    protected lateinit var sessionDataSource: SessionDataSource
    protected lateinit var sessionUnitDataSource: SessionUnitDataSource
    protected lateinit var surveillanceFormDataSource: SurveillanceFormDataSource
    protected lateinit var specimenDataSource: SpecimenDataSource
    protected lateinit var specimenImageDataSource: SpecimenImageDataSource
    protected lateinit var formAnswerDataSource: FormAnswerDataSource
    protected lateinit var dataRecoveryDataSource: DataRecoveryDataSource
    protected lateinit var dataRecoverySubmissionRepository: DataRecoverySubmissionRepository
    protected lateinit var formAnswerRepository: FormAnswerRepository
    protected lateinit var formRepository: FormRepository
    protected lateinit var formQuestionRepository: FormQuestionRepository
    protected lateinit var metadataUploadStatusRepository: MetadataUploadStatusRepository
    protected lateinit var notifier: MetadataUploadNotifier

    protected val dataRecoveryRequests = mutableListOf<PostDataRecoveryRequestDto>()
    protected val recordedRecoverySubmissions = mutableListOf<DataRecoverySubmission>()
    protected val persistedRecoveryKeys = mutableSetOf<String>()
    protected val recordedStepStatuses = mutableListOf<MetadataUploadStepStatus>()

    @Before
    fun setUp() {
        setUpAndroidMocks()
        setUpLocalRepositories()
        setUpSessionDataSources()
        setUpSpecimenDataSources()
        setUpFormAnswerMocks()
        setUpRecoveryAndStatusMocks()
    }

    protected fun setUpAndroidMocks() {
        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0

        context = mockk(relaxed = true)
        every { context.getString(any()) } returns "error"

        val completedForegroundFuture = mockk<ListenableFuture<Void>> {
            every { isDone } returns true
            every { get() } returns null
        }
        workerParams = mockk(relaxed = true)
        every { workerParams.inputData } returns workDataOf(
            "session_id" to sessionId.toString(),
            "site_id" to SITE_ID,
        )
        every { workerParams.runAttemptCount } returns 0
        every {
            workerParams.foregroundUpdater.setForegroundAsync(any(), any(), any())
        } returns completedForegroundFuture
    }

    protected fun setUpLocalRepositories() {
        deviceCache = mockk(relaxed = true)
        coEvery { deviceCache.getDevice() } returns device
        coEvery { deviceCache.getProgramId() } returns PROGRAM_ID

        programRepository = mockk()
        givenProgramFormVersion(FORM_VERSION)

        sessionRepository = mockk()
        coEvery { sessionRepository.getSessionById(sessionId) } returns session
        coEvery { sessionRepository.upsertSession(any(), any()) } returns Result.Success(Unit)

        sessionUnitRepository = mockk(relaxed = true)
        coEvery { sessionUnitRepository.getSessionUnitsForSession(any()) } returns emptyList()

        surveillanceFormRepository = mockk(relaxed = true)
        coEvery { surveillanceFormRepository.getSurveillanceFormBySessionId(any()) } returns null
        coEvery { surveillanceFormRepository.upsertSurveillanceForm(any(), any()) } returns Result.Success(Unit)

        specimenRepository = mockk()
        coEvery {
            specimenRepository.getSpecimenImagesAndInferenceResultsBySessionScope(sessionId, null)
        } returns listOf(
            specimenWithImage(SPECIMEN_1, IMAGE_1),
            specimenWithImage(SPECIMEN_2, IMAGE_2),
        )
        coEvery { specimenRepository.getSessionUnitIdForSpecimen(any(), any()) } returns null
        coEvery { specimenRepository.updateSpecimen(any(), any(), any()) } returns Result.Success(Unit)

        specimenImageRepository = mockk()
        coEvery { specimenImageRepository.updateSpecimenImage(any(), any(), any()) } returns Result.Success(Unit)

        inferenceResultRepository = mockk(relaxed = true)
    }

    protected fun setUpSessionDataSources() {
        deviceDataSource = mockk()
        coEvery { deviceDataSource.getDeviceById(DEVICE_ID) } returns Result.Success(
            DeviceDto(
                deviceId = DEVICE_ID,
                model = device.model,
                registeredAt = DEVICE_REGISTERED_AT,
                submittedAt = null,
                programId = PROGRAM_ID,
            )
        )

        sessionDataSource = mockk()
        coEvery { sessionDataSource.getSessionByFrontendId(sessionId) } returns Result.Error(NetworkError.NOT_FOUND)
        coEvery { sessionDataSource.postSession(any(), any(), any(), any()) } returns Result.Success(
            PostSessionResponseDto(
                session = SessionDto(
                    sessionId = REMOTE_SESSION_ID,
                    frontendId = sessionId,
                    completedAt = session.completedAt,
                    siteId = SITE_ID,
                    deviceId = DEVICE_ID,
                )
            )
        )

        sessionUnitDataSource = mockk()

        surveillanceFormDataSource = mockk()
        coEvery {
            surveillanceFormDataSource.getSurveillanceFormBySessionId(REMOTE_SESSION_ID)
        } returns Result.Error(NetworkError.NOT_FOUND)
        coEvery {
            surveillanceFormDataSource.postSurveillanceForm(any(), REMOTE_SESSION_ID)
        } returns Result.Success(PostSurveillanceFormResponseDto())
    }

    protected fun setUpSpecimenDataSources() {
        specimenDataSource = mockk()
        coEvery {
            specimenDataSource.getSpecimenByIdAndSessionId(any(), REMOTE_SESSION_ID)
        } returns Result.Error(NetworkError.NOT_FOUND)
        remoteSpecimenIds.forEach { (specimenId, remoteSpecimenId) ->
            coEvery {
                specimenDataSource.postSpecimen(
                    match { it.id == specimenId }, REMOTE_SESSION_ID, null, 1
                )
            } returns Result.Success(
                PostSpecimenResponseDto(
                    specimen = SpecimenDto(
                        id = remoteSpecimenId,
                        specimenId = specimenId,
                        sessionId = REMOTE_SESSION_ID,
                        expectedImages = 1,
                    )
                )
            )
        }

        specimenImageDataSource = mockk()
        coEvery {
            specimenImageDataSource.getSpecimenImageMetadata(any(), any())
        } returns Result.Error(NetworkError.NOT_FOUND)
        listOf(IMAGE_1, IMAGE_2).forEach { imageId ->
            coEvery {
                specimenImageDataSource.postSpecimenImageMetadata(
                    match { it.localId == imageId }, null, any()
                )
            } returns Result.Success(
                PostSpecimenImageResponseDto(image = SpecimenImageDto(filemd5 = imageId))
            )
        }
    }

    protected fun setUpFormAnswerMocks() {
        formAnswerRepository = mockk(relaxed = true)
        coEvery { formAnswerRepository.getSessionScopedFormAnswers(sessionId) } returns mapOf(
            QUESTION_ID to formAnswer
        )
        coEvery { formAnswerRepository.upsertFormAnswer(any(), any(), any(), any()) } returns Result.Success(Unit)

        formQuestionRepository = mockk()
        coEvery { formQuestionRepository.getFormIdByQuestionId(QUESTION_ID) } returns FORM_ID

        formRepository = mockk()
        coEvery { formRepository.getFormById(FORM_ID) } returns Form(
            id = FORM_ID, name = "Form", version = FORM_VERSION
        )

        formAnswerDataSource = mockk()
        coEvery {
            formAnswerDataSource.getFormAnswersForSession(REMOTE_SESSION_ID)
        } returns Result.Error(NetworkError.NOT_FOUND)
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returns Result.Success(PostFormAnswersResponseDto())
    }

    protected fun setUpRecoveryAndStatusMocks() {
        dataRecoveryRequests.clear()
        val dataRecoveryRequest = slot<PostDataRecoveryRequestDto>()
        dataRecoveryDataSource = mockk()
        coEvery { dataRecoveryDataSource.postDataRecovery(capture(dataRecoveryRequest)) } answers {
            dataRecoveryRequests += dataRecoveryRequest.captured
            Result.Success(
                PostDataRecoveryResponseDto(
                    dataRecovery = DataRecoveryDto(
                        id = DATA_RECOVERY_ID,
                        idempotencyKey = dataRecoveryRequest.captured.idempotencyKey,
                    )
                )
            )
        }

        recordedRecoverySubmissions.clear()
        persistedRecoveryKeys.clear()
        val recoverySubmission = slot<DataRecoverySubmission>()
        dataRecoverySubmissionRepository = mockk()
        coEvery {
            dataRecoverySubmissionRepository.getSubmittedIdempotencyKeysForSession(sessionId)
        } answers { persistedRecoveryKeys.toSet() }
        coEvery {
            dataRecoverySubmissionRepository.recordDataRecoverySubmission(capture(recoverySubmission), sessionId)
        } answers {
            recordedRecoverySubmissions += recoverySubmission.captured
            persistedRecoveryKeys += recoverySubmission.captured.idempotencyKey
            Result.Success(Unit)
        }

        recordedStepStatuses.clear()
        val stepStatus = slot<MetadataUploadStepStatus>()
        metadataUploadStatusRepository = mockk()
        coEvery {
            metadataUploadStatusRepository.upsertMetadataUploadStatus(capture(stepStatus), sessionId)
        } answers {
            recordedStepStatuses += stepStatus.captured
            Result.Success(Unit)
        }
        coEvery {
            metadataUploadStatusRepository.failInProgressMetadataUploadStatuses(sessionId)
        } answers {
            val stillInProgress = finalStepStatuses()
                .filterValues { it == UploadStatus.IN_PROGRESS }.keys
            stillInProgress.forEach { step ->
                recordedStepStatuses += MetadataUploadStepStatus(step, UploadStatus.FAILED, 0L)
            }
            Result.Success(Unit)
        }

        notifier = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    protected fun buildWorker() = MetadataUploadWorker(
        context = context,
        workerParams = workerParams,
        deviceCache = deviceCache,
        programRepository = programRepository,
        transactionHelper = mockk<TransactionHelper>(),
        sessionRepository = sessionRepository,
        sessionUnitRepository = sessionUnitRepository,
        surveillanceFormRepository = surveillanceFormRepository,
        specimenRepository = specimenRepository,
        specimenImageRepository = specimenImageRepository,
        inferenceResultRepository = inferenceResultRepository,
        deviceDataSource = deviceDataSource,
        sessionDataSource = sessionDataSource,
        sessionUnitDataSource = sessionUnitDataSource,
        surveillanceFormDataSource = surveillanceFormDataSource,
        specimenDataSource = specimenDataSource,
        specimenImageDataSource = specimenImageDataSource,
        formAnswerDataSource = formAnswerDataSource,
        dataRecoveryDataSource = dataRecoveryDataSource,
        dataRecoverySubmissionRepository = dataRecoverySubmissionRepository,
        formAnswerRepository = formAnswerRepository,
        formRepository = formRepository,
        formQuestionRepository = formQuestionRepository,
        metadataUploadStatusRepository = metadataUploadStatusRepository,
        notifier = notifier,
    )

    protected fun givenProgramFormVersion(formVersion: String?) {
        coEvery { programRepository.getProgramById(PROGRAM_ID) } returns Program(
            id = PROGRAM_ID, name = "Program", country = "Uganda", formVersion = formVersion
        )
    }

    protected fun specimenWithImage(specimenId: String, imageId: String) =
        SpecimenWithSpecimenImagesAndInferenceResults(
            specimen = Specimen(id = specimenId, remoteId = null, shouldProcessFurther = false),
            specimenImagesAndInferenceResults = listOf(
                SpecimenImageAndInferenceResult(
                    specimenImage = SpecimenImage(
                        localId = imageId,
                        remoteId = null,
                        species = null,
                        sex = null,
                        abdomenStatus = null,
                        imageUri = mockk<Uri>(),
                        metadataUploadStatus = UploadStatus.NOT_STARTED,
                        imageUploadStatus = UploadStatus.NOT_STARTED,
                        capturedAt = 0L,
                        submittedAt = null,
                        imageMetadata = null,
                    ),
                    inferenceResult = null,
                )
            )
        )

    protected fun finalStepStatuses(): Map<MetadataUploadStep, UploadStatus> =
        recordedStepStatuses.associate { it.step to it.status }

    protected fun assertSpecimenPosted(specimenId: String) {
        coVerify {
            specimenDataSource.postSpecimen(match { it.id == specimenId }, REMOTE_SESSION_ID, null, 1)
        }
    }

    protected fun assertImagePosted(imageId: String, remoteSpecimenId: Int) {
        coVerify {
            specimenImageDataSource.postSpecimenImageMetadata(
                match { it.localId == imageId },
                null,
                remoteSpecimenId
            )
        }
    }
}
