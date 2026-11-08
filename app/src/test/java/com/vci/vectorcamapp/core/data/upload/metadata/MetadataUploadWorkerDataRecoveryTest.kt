package com.vci.vectorcamapp.core.data.upload.metadata

import androidx.work.ListenableWorker
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MetadataUploadWorkerDataRecoveryTest : MetadataUploadWorkerTestBase() {

    // ========================================
    // D. Data recovery submissions are remembered
    // ========================================

    @Test
    fun worker_d01_recoveredFailure_isRememberedLocally() = runTest {
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returns Result.Error(NetworkError.SERVER_ERROR)

        buildWorker().doWork()

        val submission = recordedRecoverySubmissions.single()
        assertThat(submission.idempotencyKey).isEqualTo("$sessionId:FORM_ANSWERS:$sessionId")
        assertThat(submission.step).isEqualTo(MetadataUploadStep.FORM_ANSWERS)
        assertThat(submission.dataRecoveryId).isEqualTo(DATA_RECOVERY_ID)
    }

    @Test
    fun worker_d02_failureAlreadySentToDataRecovery_isNotSentAgain() = runTest {
        persistedRecoveryKeys += "$sessionId:FORM_ANSWERS:$sessionId"
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returns Result.Error(NetworkError.SERVER_ERROR)

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = TOTAL_ATTEMPTS_PER_CALL) {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        }
        coVerify(exactly = 0) { dataRecoveryDataSource.postDataRecovery(any()) }
        assertThat(finalStepStatuses()[MetadataUploadStep.FORM_ANSWERS]).isEqualTo(UploadStatus.RECOVERED)
    }

    @Test
    fun worker_d03_repeatedUploadsOfSameFailure_sendToDataRecoveryOnce() = runTest {
        coEvery {
            specimenDataSource.postSpecimen(match { it.id == SPECIMEN_1 }, REMOTE_SESSION_ID, null, 1)
        } returns Result.Error(NetworkError.SERVER_ERROR)

        repeat(3) { buildWorker().doWork() }

        coVerify(exactly = 3 * TOTAL_ATTEMPTS_PER_CALL) {
            specimenDataSource.postSpecimen(match { it.id == SPECIMEN_1 }, REMOTE_SESSION_ID, null, 1)
        }
        assertThat(dataRecoveryRequests.map { it.idempotencyKey })
            .containsExactly("$sessionId:SPECIMENS:$SPECIMEN_1")
    }

    @Test
    fun worker_d04_failedRecoveryReport_isSentAgainOnNextUpload() = runTest {
        coEvery {
            formAnswerDataSource.postFormAnswersForSession(REMOTE_SESSION_ID, FORM_VERSION, any(), any())
        } returns Result.Error(NetworkError.SERVER_ERROR)
        coEvery { dataRecoveryDataSource.postDataRecovery(any()) } returns Result.Error(NetworkError.SERVER_ERROR)

        buildWorker().doWork()
        buildWorker().doWork()

        coVerify(exactly = 2 * TOTAL_ATTEMPTS_PER_CALL) { dataRecoveryDataSource.postDataRecovery(any()) }
        assertThat(persistedRecoveryKeys).isEmpty()
    }
}
