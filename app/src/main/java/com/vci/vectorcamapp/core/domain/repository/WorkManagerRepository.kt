package com.vci.vectorcamapp.core.domain.repository

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface WorkManagerRepository {
    fun enqueueSessionUpload(sessionId: UUID, siteId: Int)
    fun observeActiveUploadingSessions(sessionIds: List<UUID>): Flow<Set<UUID>>
}
