package com.vci.vectorcamapp.core.data.upload.metadata

import androidx.work.ListenableWorker
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryRequestDto
import com.vci.vectorcamapp.core.data.dto.form_answer.PostFormAnswersResponseDto
import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class MetadataUploadWorkerTest : MetadataUploadWorkerTestBase() {

    // ========================================
    // A. A failing step is reported and the upload moves on
    // ========================================

    @Test
    fun worker_a01_formAnswersFail_reportsToDataRecoveryAndStillUploadsSpecimens() = runTest {
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = TOTAL_ATTEMPTS_PER_CALL) {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        }

        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.idempotencyKey).isEqualTo("$sessionId:FORM_ANSWERS:$sessionId")
        assertThat(recoveryRequest.programId).isEqualTo(PROGRAM_ID)
        assertThat(recoveryRequest.siteId).isEqualTo(SITE_ID)
        assertThat(recoveryRequest.sessionFrontendId).isEqualTo(sessionId)
        assertThat(recoveryRequest.year).isEqualTo(COLLECTION_YEAR)
        assertThat(recoveryRequest.month).isEqualTo(COLLECTION_MONTH)
        assertThat(recoveryRequest.notes).contains("sessions/$REMOTE_SESSION_ID/forms/answers")
        val payload = recoveryRequest.payload
        assertThat(payload.route).isEqualTo("sessions/$REMOTE_SESSION_ID/forms/answers")
        assertThat(payload.method).isEqualTo("POST")
        assertThat(payload.error).isEqualTo(NetworkError.SERVER_ERROR.name)
        assertThat(payload.userId).isEqualTo("${DEVICE_ID}_$DEVICE_REGISTERED_AT")
        assertThat(payload.deviceId).isEqualTo(DEVICE_ID)
        assertThat(payload.attemptCount).isEqualTo(TOTAL_ATTEMPTS_PER_CALL)
        assertThat(payload.workerAttempt).isEqualTo(1)
        assertThat(payload.failedAt).isGreaterThan(0L)
        val requestData = payload.requestData.jsonObject
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
            MetadataUploadStep.FORM_ANSWERS, UploadStatus.RECOVERED,
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

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = TOTAL_ATTEMPTS_PER_CALL) {
            surveillanceFormDataSource.postSurveillanceForm(any(), REMOTE_SESSION_ID)
        }
        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.payload.route).isEqualTo("sessions/$REMOTE_SESSION_ID/survey")
        val requestData = recoveryRequest.payload.requestData.jsonObject
        assertThat(requestData["sessionId"]?.jsonPrimitive?.int).isEqualTo(REMOTE_SESSION_ID)
        assertThat(requestData["numPeopleSleptInHouse"]?.jsonPrimitive?.int).isEqualTo(4)

        assertSpecimenPosted(SPECIMEN_1)
        assertSpecimenPosted(SPECIMEN_2)
        assertImagePosted(IMAGE_1, remoteSpecimenIds.getValue(SPECIMEN_1))
        assertImagePosted(IMAGE_2, remoteSpecimenIds.getValue(SPECIMEN_2))
        assertThat(finalStepStatuses()[MetadataUploadStep.SURVEILLANCE_FORM]).isEqualTo(UploadStatus.RECOVERED)
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
        assertThat(recoveryRequest.idempotencyKey).isEqualTo("$sessionId:SPECIMENS:$SPECIMEN_1")
        assertThat(recoveryRequest.payload.route).isEqualTo("sessions/$REMOTE_SESSION_ID/specimens")
        val requestData = recoveryRequest.payload.requestData.jsonObject
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
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMENS]).isEqualTo(UploadStatus.RECOVERED)
        // The skipped image was never sent anywhere, so it keeps the upload pending.
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMEN_IMAGES]).isEqualTo(UploadStatus.FAILED)
    }

    @Test
    fun worker_a04_specimenImageFails_reportsToDataRecoveryAndUploadsNextImage() = runTest {
        coEvery {
            specimenImageDataSource.postSpecimenImageMetadata(match { it.localId == IMAGE_1 }, null, any())
        } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.idempotencyKey).isEqualTo("$sessionId:SPECIMEN_IMAGES:$IMAGE_1")
        assertThat(recoveryRequest.payload.route)
            .isEqualTo("specimens/${remoteSpecimenIds.getValue(SPECIMEN_1)}/images/data")
        assertThat(recoveryRequest.payload.requestData.jsonObject["filemd5"]?.jsonPrimitive?.content)
            .isEqualTo(IMAGE_1)

        assertImagePosted(IMAGE_2, remoteSpecimenIds.getValue(SPECIMEN_2))
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMENS]).isEqualTo(UploadStatus.COMPLETED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMEN_IMAGES]).isEqualTo(UploadStatus.RECOVERED)
    }

    @Test
    fun worker_a05_deviceSyncFails_reportsToDataRecoveryAndUploadsWithCachedDevice() = runTest {
        coEvery { deviceDataSource.getDeviceById(DEVICE_ID) } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        val recoveryRequest = dataRecoveryRequests.single()
        assertThat(recoveryRequest.payload.route).isEqualTo("devices/register")
        assertThat(recoveryRequest.payload.requestData.jsonObject["programId"]?.jsonPrimitive?.int)
            .isEqualTo(PROGRAM_ID)

        coVerify { sessionDataSource.postSession(any(), SITE_ID, DEVICE_ID, 2) }
        assertSpecimenPosted(SPECIMEN_1)
        assertSpecimenPosted(SPECIMEN_2)
        assertThat(finalStepStatuses()[MetadataUploadStep.DEVICE]).isEqualTo(UploadStatus.RECOVERED)
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

        assertThat(dataRecoveryRequests.map { it.payload.route to it.payload.error }).containsExactly(
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
        assertThat(recordedRecoverySubmissions).isEmpty()
        assertThat(finalStepStatuses()[MetadataUploadStep.FORM_ANSWERS]).isEqualTo(UploadStatus.FAILED)
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
        assertThat(recoveryRequest.payload.route).isEqualTo("sessions")
        val requestData = recoveryRequest.payload.requestData.jsonObject
        assertThat(requestData["frontendId"]?.jsonPrimitive?.content).isEqualTo(sessionId.toString())
        assertThat(requestData["siteId"]?.jsonPrimitive?.int).isEqualTo(SITE_ID)
        assertThat(requestData["deviceId"]?.jsonPrimitive?.int).isEqualTo(DEVICE_ID)

        coVerify(exactly = 0) { formAnswerDataSource.postFormAnswersForSession(any(), any(), any(), any()) }
        coVerify(exactly = 0) { specimenDataSource.postSpecimen(any(), any(), any(), any()) }
        coVerify(exactly = 0) { specimenImageDataSource.postSpecimenImageMetadata(any(), any(), any()) }
        assertThat(finalStepStatuses()).containsExactly(
            MetadataUploadStep.DEVICE, UploadStatus.COMPLETED,
            MetadataUploadStep.SESSION, UploadStatus.RECOVERED,
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
        val dataRecoveryRequest = slot<PostDataRecoveryRequestDto>()
        coEvery { dataRecoveryDataSource.postDataRecovery(capture(dataRecoveryRequest)) } answers {
            dataRecoveryRequests += dataRecoveryRequest.captured
            Result.Error(NetworkError.SERVER_ERROR)
        }

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(dataRecoveryRequests.map { it.payload.workerAttempt }.distinct())
            .containsExactly(MetadataUploadWorker.MAX_RETRIES + 1)
        assertThat(finalStepStatuses()[MetadataUploadStep.FORM_ANSWERS]).isEqualTo(UploadStatus.FAILED)
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
}
