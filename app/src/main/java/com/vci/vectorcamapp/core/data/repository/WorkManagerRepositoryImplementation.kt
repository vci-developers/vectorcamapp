package com.vci.vectorcamapp.core.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.vci.vectorcamapp.core.data.upload.UploadForegroundService
import com.vci.vectorcamapp.core.data.upload.image.ImageUploadWorker
import com.vci.vectorcamapp.core.data.upload.metadata.MetadataUploadWorker
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkManagerRepositoryImplementation @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) : WorkManagerRepository {

    companion object {
        private const val UPLOAD_WORK_CHAIN_PREFIX = "session_upload_chain_"
    }

    private val uploadConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun enqueueSessionUpload(sessionId: UUID, siteId: Int) {
        val chainName = UPLOAD_WORK_CHAIN_PREFIX + sessionId
        workManager.beginUniqueWork(
            chainName,
            ExistingWorkPolicy.REPLACE,
            buildSessionMetadataWork(sessionId, siteId)
        ).then(buildSessionImageWork(sessionId)).enqueue()
        // Start the sticky foreground service after work is enqueued so the process
        // stays alive even while workers are queued waiting for constraints to be met.
        UploadForegroundService.start(context)
    }

    override fun observeIsSessionActivelyUploading(sessionId: UUID): Flow<Boolean> {
        val chainName = "session_upload_chain_$sessionId"
        return workManager.getWorkInfosForUniqueWorkFlow(chainName)
            .map { workInfos ->
                workInfos.any {
                    it.state == WorkInfo.State.RUNNING
                }
            }
    }

    private fun buildSessionMetadataWork(sessionId: UUID, siteId: Int): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<MetadataUploadWorker>()
            .addTag(UploadForegroundService.UPLOAD_WORK_TAG)
            .setInputData(workDataOf(
                "session_id" to sessionId.toString(),
                "site_id" to siteId,
            ))
            .setConstraints(uploadConstraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
    }

    private fun buildSessionImageWork(sessionId: UUID): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .addTag(UploadForegroundService.UPLOAD_WORK_TAG)
            .setInputData(workDataOf(
                ImageUploadWorker.KEY_SESSION_ID to sessionId.toString()
            ))
            .setConstraints(uploadConstraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
    }
}
