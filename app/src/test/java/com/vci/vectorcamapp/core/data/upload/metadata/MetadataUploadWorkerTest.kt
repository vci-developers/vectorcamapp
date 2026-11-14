package com.vci.vectorcamapp.core.data.upload.metadata

import androidx.work.ListenableWorker
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryRequestDto
import com.vci.vectorcamapp.core.data.dto.form_answer.PostFormAnswersResponseDto
import com.vci.vectorcamapp.core.data.dto.session_unit.GetSessionUnitsResponseDto
import com.vci.vectorcamapp.core.domain.model.SessionUnit
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
import java.util.UUID

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

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = TOTAL_ATTEMPTS_PER_CALL) {
            specimenDataSource.postSpecimen(match { it.id == SPECIMEN_1 }, REMOTE_SESSION_ID, null, 1)
        }
        assertThat(dataRecoveryRequests.map { it.idempotencyKey }).containsExactly(
            "$sessionId:SPECIMENS:$SPECIMEN_1",
            "$sessionId:SPECIMEN_IMAGES:$IMAGE_1",
        )
        val specimenRecovery = dataRecoveryRequests.single {
            it.idempotencyKey == "$sessionId:SPECIMENS:$SPECIMEN_1"
        }
        assertThat(specimenRecovery.payload.route).isEqualTo("sessions/$REMOTE_SESSION_ID/specimens")
        val requestData = specimenRecovery.payload.requestData.jsonObject
        assertThat(requestData["specimenId"]?.jsonPrimitive?.content).isEqualTo(SPECIMEN_1)
        assertThat(requestData["expectedImages"]?.jsonPrimitive?.int).isEqualTo(1)
        val imageRecovery = dataRecoveryRequests.single {
            it.idempotencyKey == "$sessionId:SPECIMEN_IMAGES:$IMAGE_1"
        }
        assertThat(imageRecovery.payload.route)
            .isEqualTo("sessions/$REMOTE_SESSION_ID/specimens/$SPECIMEN_1/images/data")
        assertThat(imageRecovery.payload.requestData.jsonObject["clientSpecimenId"]?.jsonPrimitive?.content)
            .isEqualTo(SPECIMEN_1)

        coVerify(exactly = 0) {
            specimenImageDataSource.postSpecimenImageMetadata(match { it.localId == IMAGE_1 }, any(), any())
        }
        coVerify {
            specimenImageRepository.updateSpecimenImage(
                match { it.localId == IMAGE_1 && it.metadataUploadStatus == UploadStatus.RECOVERED },
                SPECIMEN_1,
                sessionId,
            )
        }
        assertSpecimenPosted(SPECIMEN_2)
        assertImagePosted(IMAGE_2, remoteSpecimenIds.getValue(SPECIMEN_2))
        assertThat(finalStepStatuses()[MetadataUploadStep.FORM_ANSWERS]).isEqualTo(UploadStatus.COMPLETED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMENS]).isEqualTo(UploadStatus.RECOVERED)
        // Image JSON is recovered so the metadata worker can succeed and TUS can still run.
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMEN_IMAGES]).isEqualTo(UploadStatus.RECOVERED)
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
            "sessions/$REMOTE_SESSION_ID/specimens/$SPECIMEN_1/images/data" to NetworkError.CLIENT_ERROR.name,
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
    // B. Parent failures recover remaining children so image TUS can still run
    // ========================================

    @Test
    fun worker_b01_sessionFails_recoversRemainingGraphWithoutPostingChildren() = runTest {
        coEvery { sessionDataSource.postSession(any(), any(), any(), any()) } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(dataRecoveryRequests.map { it.idempotencyKey }).containsExactly(
            "$sessionId:SESSION:$sessionId",
            "$sessionId:FORM_ANSWERS:$sessionId",
            "$sessionId:SPECIMENS:$SPECIMEN_1",
            "$sessionId:SPECIMEN_IMAGES:$IMAGE_1",
            "$sessionId:SPECIMENS:$SPECIMEN_2",
            "$sessionId:SPECIMEN_IMAGES:$IMAGE_2",
        ).inOrder()
        val sessionRecovery = dataRecoveryRequests.first()
        assertThat(sessionRecovery.payload.route).isEqualTo("sessions")
        val requestData = sessionRecovery.payload.requestData.jsonObject
        assertThat(requestData["frontendId"]?.jsonPrimitive?.content).isEqualTo(sessionId.toString())
        assertThat(requestData["siteId"]?.jsonPrimitive?.int).isEqualTo(SITE_ID)
        assertThat(requestData["deviceId"]?.jsonPrimitive?.int).isEqualTo(DEVICE_ID)

        val formRecovery = dataRecoveryRequests.single {
            it.idempotencyKey == "$sessionId:FORM_ANSWERS:$sessionId"
        }
        assertThat(formRecovery.sessionFrontendId).isEqualTo(sessionId)
        assertThat(formRecovery.payload.route).isEqualTo("sessions/$sessionId/forms/answers")
        val formRecoveryData = formRecovery.payload.requestData.jsonObject
        assertThat(formRecoveryData["formVersion"]?.jsonPrimitive?.content).isEqualTo(FORM_VERSION)
        assertThat(
            formRecoveryData["answers"]?.jsonArray?.single()?.jsonObject?.get("frontendId")
                ?.jsonPrimitive?.content
        ).isEqualTo(formAnswer.localId.toString())

        val imageRecovery = dataRecoveryRequests.single {
            it.idempotencyKey == "$sessionId:SPECIMEN_IMAGES:$IMAGE_1"
        }
        assertThat(imageRecovery.payload.route)
            .isEqualTo("sessions/$sessionId/specimens/$SPECIMEN_1/images/data")
        assertThat(imageRecovery.payload.requestData.jsonObject["clientSpecimenId"]?.jsonPrimitive?.content)
            .isEqualTo(SPECIMEN_1)

        coVerify(exactly = 0) { formAnswerDataSource.postFormAnswersForSession(any(), any(), any(), any()) }
        coVerify(exactly = 0) { specimenDataSource.postSpecimen(any(), any(), any(), any()) }
        coVerify(exactly = 0) { specimenImageDataSource.postSpecimenImageMetadata(any(), any(), any()) }
        assertThat(finalStepStatuses()).containsExactly(
            MetadataUploadStep.DEVICE, UploadStatus.COMPLETED,
            MetadataUploadStep.SESSION, UploadStatus.RECOVERED,
            MetadataUploadStep.FORM_ANSWERS, UploadStatus.RECOVERED,
            MetadataUploadStep.SPECIMENS, UploadStatus.RECOVERED,
            MetadataUploadStep.SPECIMEN_IMAGES, UploadStatus.RECOVERED,
        )
    }

    @Test
    fun worker_b02_sessionFailsOnFinalAttempt_stillRecoversGraphSoImageUploadCanRun() = runTest {
        every { workerParams.runAttemptCount } returns MetadataUploadWorker.MAX_RETRIES
        coEvery { sessionDataSource.postSession(any(), any(), any(), any()) } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(finalStepStatuses()[MetadataUploadStep.SESSION]).isEqualTo(UploadStatus.RECOVERED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMEN_IMAGES]).isEqualTo(UploadStatus.RECOVERED)
    }

    @Test
    fun worker_b04_sessionUnitFails_recoversFormAnswersAndLinkedSpecimensWithoutPosting() = runTest {
        val unitId = UUID.randomUUID()
        val unit = SessionUnit(localId = unitId, remoteId = null, unitOrder = 1, createdAt = 1_000L)
        coEvery { sessionUnitRepository.getSessionUnitsForSession(sessionId) } returns listOf(unit)
        coEvery { sessionUnitDataSource.getSessionUnitForSession(REMOTE_SESSION_ID) } returns
            Result.Success(GetSessionUnitsResponseDto(sessionId = REMOTE_SESSION_ID, units = emptyList()))
        coEvery { sessionUnitDataSource.postSessionUnit(unit, REMOTE_SESSION_ID) } returns
            Result.Error(NetworkError.SERVER_ERROR)
        coEvery { specimenRepository.getSessionUnitIdForSpecimen(SPECIMEN_1, sessionId) } returns unitId
        coEvery { formAnswerRepository.getSessionUnitScopedFormAnswers(unitId) } returns mapOf(
            QUESTION_ID to formAnswer
        )

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 0) {
            formAnswerDataSource.postFormAnswersForSession(any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            specimenDataSource.postSpecimen(match { it.id == SPECIMEN_1 }, any(), any(), any())
        }
        assertSpecimenPosted(SPECIMEN_2)

        val formRecovery = dataRecoveryRequests.single {
            it.idempotencyKey == "$sessionId:FORM_ANSWERS:$sessionId"
        }
        val unitAnswer = formRecovery.payload.requestData.jsonObject["answers"]?.jsonArray
            ?.map { it.jsonObject }
            ?.single { it["sessionUnitFrontendId"] != null }
        assertThat(unitAnswer?.get("sessionUnitFrontendId")?.jsonPrimitive?.content)
            .isEqualTo(unitId.toString())
        assertThat(unitAnswer?.get("frontendId")?.jsonPrimitive?.content)
            .isEqualTo(formAnswer.localId.toString())

        val specimenRecovery = dataRecoveryRequests.single {
            it.idempotencyKey == "$sessionId:SPECIMENS:$SPECIMEN_1"
        }
        assertThat(
            specimenRecovery.payload.requestData.jsonObject["sessionUnitFrontendId"]
                ?.jsonPrimitive?.content
        ).isEqualTo(unitId.toString())
        val imageRecovery = dataRecoveryRequests.single {
            it.idempotencyKey == "$sessionId:SPECIMEN_IMAGES:$IMAGE_1"
        }
        assertThat(imageRecovery.payload.requestData.jsonObject["clientSpecimenId"]?.jsonPrimitive?.content)
            .isEqualTo(SPECIMEN_1)

        assertThat(finalStepStatuses()[MetadataUploadStep.SESSION_UNITS]).isEqualTo(UploadStatus.RECOVERED)
        assertThat(finalStepStatuses()[MetadataUploadStep.FORM_ANSWERS]).isEqualTo(UploadStatus.RECOVERED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMENS]).isEqualTo(UploadStatus.RECOVERED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMEN_IMAGES]).isEqualTo(UploadStatus.RECOVERED)
    }

    @Test
    fun worker_b05_sessionFailsForLegacySurveillance_recoversSurveyWithoutPosting() = runTest {
        givenProgramFormVersion(null)
        coEvery { formAnswerRepository.getSessionScopedFormAnswers(sessionId) } returns emptyMap()
        coEvery { surveillanceFormRepository.getSurveillanceFormBySessionId(sessionId) } returns surveillanceForm
        coEvery { sessionDataSource.postSession(any(), any(), any(), any()) } returns
            Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        val surveyRecovery = dataRecoveryRequests.single { it.payload.route.contains("/survey") }
        assertThat(surveyRecovery.payload.route).isEqualTo("sessions/$sessionId/survey")
        assertThat(surveyRecovery.payload.requestData.jsonObject.containsKey("sessionId")).isFalse()
        assertThat(
            surveyRecovery.payload.requestData.jsonObject["numPeopleSleptInHouse"]?.jsonPrimitive?.int
        ).isEqualTo(4)
        coVerify(exactly = 0) { surveillanceFormDataSource.postSurveillanceForm(any(), any()) }
        assertThat(finalStepStatuses()[MetadataUploadStep.SURVEILLANCE_FORM])
            .isEqualTo(UploadStatus.RECOVERED)
        assertThat(finalStepStatuses()[MetadataUploadStep.SPECIMEN_IMAGES])
            .isEqualTo(UploadStatus.RECOVERED)
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

    @Test
    fun worker_c02_interruptedImageStatuses_areRevertedToPendingBeforeUpload() = runTest {
        val interrupted = specimenWithImage(SPECIMEN_1, IMAGE_1)
        val interruptedImage = interrupted.specimenImagesAndInferenceResults.single().specimenImage.copy(
            metadataUploadStatus = UploadStatus.IN_PROGRESS,
            imageUploadStatus = UploadStatus.IN_PROGRESS,
        )
        coEvery {
            specimenRepository.getSpecimenImagesAndInferenceResultsBySessionScope(sessionId, null)
        } returns listOf(
            interrupted.copy(
                specimenImagesAndInferenceResults = listOf(
                    interrupted.specimenImagesAndInferenceResults.single().copy(
                        specimenImage = interruptedImage
                    )
                )
            ),
            specimenWithImage(SPECIMEN_2, IMAGE_2),
        )

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify {
            specimenImageRepository.updateSpecimenImage(
                match {
                    it.localId == IMAGE_1 &&
                        it.metadataUploadStatus == UploadStatus.NOT_STARTED &&
                        it.imageUploadStatus == UploadStatus.NOT_STARTED
                },
                SPECIMEN_1,
                sessionId,
            )
        }
    }
}
