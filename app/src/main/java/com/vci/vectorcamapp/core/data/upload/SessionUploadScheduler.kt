package com.vci.vectorcamapp.core.data.upload

import com.vci.vectorcamapp.core.domain.model.helpers.SessionUploadDelivery
import com.vci.vectorcamapp.core.domain.repository.MetadataUploadStatusRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionUploadScheduler @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val specimenRepository: SpecimenRepository,
    private val metadataUploadStatusRepository: MetadataUploadStatusRepository,
    private val workManagerRepository: WorkManagerRepository,
) {
    suspend fun enqueuePendingCompleteSessions(): Int {
        val completeSessions = sessionRepository.observeCompleteSessionsAndSites().first()
        var enqueuedCount = 0
        for (sessionAndSite in completeSessions) {
            val session = sessionAndSite.session
            val specimens = specimenRepository.getSpecimenImagesAndInferenceResultsBySessionScope(
                session.localId,
                null,
            )
            val images = specimens.flatMap { specimen ->
                specimen.specimenImagesAndInferenceResults.map { it.specimenImage }
            }
            val metadataSteps = metadataUploadStatusRepository
                .observeMetadataUploadStatusesForSession(session.localId)
                .first()
            if (SessionUploadDelivery.isFullyDelivered(session.submittedAt, metadataSteps, images)) {
                continue
            }
            Timber.tag(TAG).i(
                "Enqueueing remaining upload for session ${session.localId}"
            )
            workManagerRepository.enqueueSessionUpload(session.localId, sessionAndSite.site.id)
            enqueuedCount++
        }
        return enqueuedCount
    }

    companion object {
        private const val TAG = "DataRecovery"
    }
}
