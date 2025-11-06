package com.vci.vectorcamapp.core.domain.repository

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface WorkManagerRepository {
    fun enqueueSessionUpload(sessionId: UUID, siteId: Int)
    fun observeIsSessionActivelyUploading(sessionId: UUID): Flow<Boolean>
    fun appendImageWorkToChain(sessionId: UUID, siteId: Int)
    fun appendMetadataWorkToChain(sessionId: UUID, siteId: Int)
}
