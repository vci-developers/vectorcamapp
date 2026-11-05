package com.vci.vectorcamapp.core.data.upload.metadata

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryRequestDto
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

class MetadataUploadWorkerTest {

    private companion object {
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
    }

    private val sessionId: UUID = UUID.randomUUID()
    private val remoteSpecimenIds = mapOf(SPECIMEN_1 to 100, SPECIMEN_2 to 200)

    private val device = Device(
        id = DEVICE_ID, model = "Pixel", registeredAt = DEVICE_REGISTERED_AT, submittedAt = null
    )
    private val session = Session(
        localId = sessionId,
        remoteId = null,
        hardwareId = "HW001",
        collectorTitle = "Dr.",
        collectorName = "Alice",
        collectorLastTrainedOn = 0L,
        collectionDate = 1_000_000L,
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
    private val formAnswer = FormAnswer(
        localId = UUID.randomUUID(),
        remoteId = null,
        value = FormAnswerValue.StringValue("yes"),
        dataType = "string",
        submittedAt = 1_000_300L,
    )
    private val surveillanceForm = SurveillanceForm(
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

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var deviceCache: DeviceCache
    private lateinit var programRepository: ProgramRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionUnitRepository: SessionUnitRepository
    private lateinit var surveillanceFormRepository: SurveillanceFormRepository
    private lateinit var specimenRepository: SpecimenRepository
    private lateinit var specimenImageRepository: SpecimenImageRepository
    private lateinit var inferenceResultRepository: InferenceResultRepository
    private lateinit var deviceDataSource: DeviceDataSource
    private lateinit var sessionDataSource: SessionDataSource
    private lateinit var sessionUnitDataSource: SessionUnitDataSource
    private lateinit var surveillanceFormDataSource: SurveillanceFormDataSource
    private lateinit var specimenDataSource: SpecimenDataSource
    private lateinit var specimenImageDataSource: SpecimenImageDataSource
    private lateinit var formAnswerDataSource: FormAnswerDataSource
    private lateinit var dataRecoveryDataSource: DataRecoveryDataSource
    private lateinit var formAnswerRepository: FormAnswerRepository
    private lateinit var formRepository: FormRepository
    private lateinit var formQuestionRepository: FormQuestionRepository
    private lateinit var metadataUploadStatusRepository: MetadataUploadStatusRepository
    private lateinit var notifier: MetadataUploadNotifier

    private val dataRecoveryRequests = mutableListOf<PostDataRecoveryRequestDto>()
    private val recordedStepStatuses = mutableListOf<MetadataUploadStepStatus>()

    @Before
    fun setUp() {
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

        dataRecoveryRequests.clear()
        val dataRecoveryRequest = slot<PostDataRecoveryRequestDto>()
        dataRecoveryDataSource = mockk()
        coEvery { dataRecoveryDataSource.postDataRecovery(capture(dataRecoveryRequest)) } answers {
            dataRecoveryRequests += dataRecoveryRequest.captured
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

    // ========================================
    // A. A failing step is reported and the upload moves on
    // ========================================

    @Test
    fun worker_a01_formAnswersFail_reportsToDataRecoveryAndStillUploadsSpecimens() = runTest {
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        coVerify(exactly = TOTAL_ATTEMPTS_PER_CALL) {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        }

        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.route).isEqualTo("sessions/$REMOTE_SESSION_ID/forms/answers")
        assertThat(recoveryRequest.method).isEqualTo("POST")
        assertThat(recoveryRequest.error).isEqualTo(NetworkError.SERVER_ERROR.name)
        assertThat(recoveryRequest.userId).isEqualTo("${DEVICE_ID}_$DEVICE_REGISTERED_AT")
        assertThat(recoveryRequest.deviceId).isEqualTo(DEVICE_ID)
        assertThat(recoveryRequest.programId).isEqualTo(PROGRAM_ID)
        assertThat(recoveryRequest.sessionFrontendId).isEqualTo(sessionId)
        assertThat(recoveryRequest.attemptCount).isEqualTo(TOTAL_ATTEMPTS_PER_CALL)
        assertThat(recoveryRequest.workerAttempt).isEqualTo(1)
        assertThat(recoveryRequest.failedAt).isGreaterThan(0L)
        val requestData = recoveryRequest.requestData.jsonObject
        assertThat(requestData["formVersion"]?.jsonPrimitive?.content).isEqualTo(FORM_VERSION)
        val answer = requestData["answers"]!!.jsonArray.single().jsonObject
        assertThat(answer["questionId"]?.jsonPrimitive?.int).isEqualTo(QUESTION_ID)
        assertThat(answer["frontendId"]?.jsonPrimitive?.content).isEqualTo(formAnswer.localId.toString())

        assertSpecimenPosted(SPECIMEN_1)
        assertSpecimenPosted(SPECIMEN_2)
        assertImagePosted(IMAGE_1, remoteSpecimenIds.getValue(SPECIMEN_1))
        assertImagePosted(IMAGE_2, remoteSpecimenIds.getValue(SPECIMEN_2))
        assertThat(finalStepStatuses()).containsExactly(
            MetadataUploadStep.DEVICE, UploadStatus.COMPLETED,
            MetadataUploadStep.SESSION, UploadStatus.COMPLETED,
            MetadataUploadStep.FORM_ANSWERS, UploadStatus.FAILED,
            MetadataUploadStep.SPECIMENS, UploadStatus.COMPLETED,
            MetadataUploadStep.SPECIMEN_IMAGES, UploadStatus.COMPLETED,
        )
    }

    @Test
    fun worker_a02_surveillanceFormFails_reportsToDataRecoveryAndStillUploadsSpecimens() = runTest {
        givenProgramFormVersion(null)
        coEvery { surveillanceFormRepository.getSurveillanceFormBySessionId(sessionId) } returns surveillanceForm
        coEvery {
            surveillanceFormDataSource.postSurveillanceForm(any(), REMOTE_SESSION_ID)
        } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        coVerify(exactly = TOTAL_ATTEMPTS_PER_CALL) {
            surveillanceFormDataSource.postSurveillanceForm(any(), REMOTE_SESSION_ID)
        }
        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.route).isEqualTo("sessions/$REMOTE_SESSION_ID/survey")
        val requestData = recoveryRequest.requestData.jsonObject
        assertThat(requestData["sessionId"]?.jsonPrimitive?.int).isEqualTo(REMOTE_SESSION_ID)
        assertThat(requestData["numPeopleSleptInHouse"]?.jsonPrimitive?.int).isEqualTo(4)

        assertSpecimenPosted(SPECIMEN_1)
        assertSpecimenPosted(SPECIMEN_2)
        assertImagePosted(IMAGE_1, remoteSpecimenIds.getValue(SPECIMEN_1))
        assertImagePosted(IMAGE_2, remoteSpecimenIds.getValue(SPECIMEN_2))
        assertThat(finalStepStatuses()[MetadataUploadStep.SURVEILLANCE_FORM]).isEqualTo(UploadStatus.FAILED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMENS]).isEqualTo(UploadStatus.COMPLETED)
    }

    @Test
    fun worker_a03_specimenFails_reportsToDataRecoveryAndUploadsNextSpecimen() = runTest {
        coEvery {
            specimenDataSource.postSpecimen(match { it.id == SPECIMEN_1 }, REMOTE_SESSION_ID, null, 1)
        } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        coVerify(exactly = TOTAL_ATTEMPTS_PER_CALL) {
            specimenDataSource.postSpecimen(match { it.id == SPECIMEN_1 }, REMOTE_SESSION_ID, null, 1)
        }
        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.route).isEqualTo("sessions/$REMOTE_SESSION_ID/specimens")
        val requestData = recoveryRequest.requestData.jsonObject
        assertThat(requestData["specimenId"]?.jsonPrimitive?.content).isEqualTo(SPECIMEN_1)
        assertThat(requestData["expectedImages"]?.jsonPrimitive?.int).isEqualTo(1)

        coVerify(exactly = 0) {
            specimenImageDataSource.postSpecimenImageMetadata(match { it.localId == IMAGE_1 }, any(), any())
        }
        coVerify {
            specimenImageRepository.updateSpecimenImage(
                match { it.localId == IMAGE_1 && it.metadataUploadStatus == UploadStatus.FAILED },
                SPECIMEN_1,
                sessionId,
            )
        }
        assertSpecimenPosted(SPECIMEN_2)
        assertImagePosted(IMAGE_2, remoteSpecimenIds.getValue(SPECIMEN_2))
        assertThat(finalStepStatuses()[MetadataUploadStep.FORM_ANSWERS]).isEqualTo(UploadStatus.COMPLETED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMENS]).isEqualTo(UploadStatus.FAILED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMEN_IMAGES]).isEqualTo(UploadStatus.FAILED)
    }

    @Test
    fun worker_a04_specimenImageFails_reportsToDataRecoveryAndUploadsNextImage() = runTest {
        coEvery {
            specimenImageDataSource.postSpecimenImageMetadata(match { it.localId == IMAGE_1 }, null, any())
        } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.route)
            .isEqualTo("specimens/${remoteSpecimenIds.getValue(SPECIMEN_1)}/images/data")
        assertThat(recoveryRequest.requestData.jsonObject["filemd5"]?.jsonPrimitive?.content)
            .isEqualTo(IMAGE_1)

        assertImagePosted(IMAGE_2, remoteSpecimenIds.getValue(SPECIMEN_2))
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMENS]).isEqualTo(UploadStatus.COMPLETED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMEN_IMAGES]).isEqualTo(UploadStatus.FAILED)
    }

    @Test
    fun worker_a05_deviceSyncFails_reportsToDataRecoveryAndUploadsWithCachedDevice() = runTest {
        coEvery { deviceDataSource.getDeviceById(DEVICE_ID) } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.route).isEqualTo("devices/register")
        assertThat(recoveryRequest.requestData.jsonObject["programId"]?.jsonPrimitive?.int)
            .isEqualTo(PROGRAM_ID)

        coVerify { sessionDataSource.postSession(any(), SITE_ID, DEVICE_ID, 2) }
        assertSpecimenPosted(SPECIMEN_1)
        assertSpecimenPosted(SPECIMEN_2)
        assertThat(finalStepStatuses()[MetadataUploadStep.DEVICE]).isEqualTo(UploadStatus.FAILED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SESSION]).isEqualTo(UploadStatus.COMPLETED)
    }

    @Test
    fun worker_a06_multipleStepsFail_reportsEachFailedRouteOnce() = runTest {
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returns Result.Error(NetworkError.SERVER_ERROR)
        coEvery {
            specimenDataSource.postSpecimen(match { it.id == SPECIMEN_1 }, REMOTE_SESSION_ID, null, 1)
        } returns Result.Error(NetworkError.NO_INTERNET)

        buildWorker().doWork()

        assertThat(dataRecoveryRequests.map { it.route to it.error }).containsExactly(
            "sessions/$REMOTE_SESSION_ID/forms/answers" to NetworkError.SERVER_ERROR.name,
            "sessions/$REMOTE_SESSION_ID/specimens" to NetworkError.NO_INTERNET.name,
        ).inOrder()
        assertSpecimenPosted(SPECIMEN_2)
        assertImagePosted(IMAGE_2, remoteSpecimenIds.getValue(SPECIMEN_2))
    }

    @Test
    fun worker_a07_dataRecoveryCallFails_isRetriedAndUploadContinues() = runTest {
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returns Result.Error(NetworkError.SERVER_ERROR)
        coEvery { dataRecoveryDataSource.postDataRecovery(any()) } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        coVerify(exactly = TOTAL_ATTEMPTS_PER_CALL) { dataRecoveryDataSource.postDataRecovery(any()) }
        assertSpecimenPosted(SPECIMEN_1)
        assertSpecimenPosted(SPECIMEN_2)
    }

    @Test
    fun worker_a08_stepRecoversWithinRetries_isNotReportedToDataRecovery() = runTest {
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returnsMany listOf(
            Result.Error(NetworkError.SERVER_ERROR),
            Result.Error(NetworkError.SERVER_ERROR),
            Result.Success(PostFormAnswersResponseDto()),
        )

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 3) {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        }
        assertThat(dataRecoveryRequests).isEmpty()
        assertThat(finalStepStatuses()[MetadataUploadStep.FORM_ANSWERS]).isEqualTo(UploadStatus.COMPLETED)
    }

    // ========================================
    // B. Session failure is the only step that stops the upload
    // ========================================

    @Test
    fun worker_b01_sessionFails_reportsToDataRecoveryAndSkipsSessionScopedEndpoints() = runTest {
        coEvery { sessionDataSource.postSession(any(), any(), any(), any()) } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.route).isEqualTo("sessions")
        val requestData = recoveryRequest.requestData.jsonObject
        assertThat(requestData["frontendId"]?.jsonPrimitive?.content).isEqualTo(sessionId.toString())
        assertThat(requestData["siteId"]?.jsonPrimitive?.int).isEqualTo(SITE_ID)
        assertThat(requestData["deviceId"]?.jsonPrimitive?.int).isEqualTo(DEVICE_ID)

        coVerify(exactly = 0) { formAnswerDataSource.postFormAnswersForSession(any(), any(), any(), any()) }
        coVerify(exactly = 0) { specimenDataSource.postSpecimen(any(), any(), any(), any()) }
        coVerify(exactly = 0) { specimenImageDataSource.postSpecimenImageMetadata(any(), any(), any()) }
        assertThat(finalStepStatuses()).containsExactly(
            MetadataUploadStep.DEVICE, UploadStatus.COMPLETED,
            MetadataUploadStep.SESSION, UploadStatus.FAILED,
            MetadataUploadStep.FORM_ANSWERS, UploadStatus.FAILED,
            MetadataUploadStep.SPECIMENS, UploadStatus.FAILED,
            MetadataUploadStep.SPECIMEN_IMAGES, UploadStatus.FAILED,
        )
    }

    @Test
    fun worker_b02_sessionFailsOnFinalAttempt_returnsFailure() = runTest {
        every { workerParams.runAttemptCount } returns MetadataUploadWorker.MAX_RETRIES
        coEvery { sessionDataSource.postSession(any(), any(), any(), any()) } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun worker_b03_nonSessionFailureOnFinalAttempt_returnsSuccessSoImageUploadRuns() = runTest {
        every { workerParams.runAttemptCount } returns MetadataUploadWorker.MAX_RETRIES
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(dataRecoveryRequests.single().workerAttempt).isEqualTo(MetadataUploadWorker.MAX_RETRIES + 1)
    }

    // ========================================
    // C. Happy path
    // ========================================

    @Test
    fun worker_c01_allStepsSucceed_doesNotCallDataRecoveryAndReturnsSuccess() = runTest {
        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 0) { dataRecoveryDataSource.postDataRecovery(any()) }
        val applicableSteps = listOf(
            MetadataUploadStep.DEVICE,
            MetadataUploadStep.SESSION,
            MetadataUploadStep.FORM_ANSWERS,
            MetadataUploadStep.SPECIMENS,
            MetadataUploadStep.SPECIMEN_IMAGES,
        )
        assertThat(recordedStepStatuses.take(applicableSteps.size).map { it.step to it.status })
            .containsExactlyElementsIn(applicableSteps.map { it to UploadStatus.IN_PROGRESS })
        assertThat(finalStepStatuses())
            .containsExactlyEntriesIn(applicableSteps.associateWith { UploadStatus.COMPLETED })
    }

    private fun buildWorker() = MetadataUploadWorker(
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
        formAnswerRepository = formAnswerRepository,
        formRepository = formRepository,
        formQuestionRepository = formQuestionRepository,
        metadataUploadStatusRepository = metadataUploadStatusRepository,
        notifier = notifier,
    )

    private fun givenProgramFormVersion(formVersion: String?) {
        coEvery { programRepository.getProgramById(PROGRAM_ID) } returns Program(
            id = PROGRAM_ID, name = "Program", country = "Uganda", formVersion = formVersion
        )
    }

    private fun specimenWithImage(specimenId: String, imageId: String) =
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

    private fun finalStepStatuses(): Map<MetadataUploadStep, UploadStatus> =
        recordedStepStatuses.associate { it.step to it.status }

    private fun assertSpecimenPosted(specimenId: String) {
        coVerify {
            specimenDataSource.postSpecimen(match { it.id == specimenId }, REMOTE_SESSION_ID, null, 1)
        }
    }

    private fun assertImagePosted(imageId: String, remoteSpecimenId: Int) {
        coVerify {
            specimenImageDataSource.postSpecimenImageMetadata(
                match { it.localId == imageId }, null, remoteSpecimenId
            )
        }
    }
}
