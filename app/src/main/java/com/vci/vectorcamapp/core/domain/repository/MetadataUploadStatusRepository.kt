package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface MetadataUploadStatusRepository {
    suspend fun upsertMetadataUploadStatus(
        metadataUploadStepStatus: MetadataUploadStepStatus,
        sessionId: UUID,
    ): Result<Unit, RoomDbError>
    suspend fun failInProgressMetadataUploadStatuses(sessionId: UUID): Result<Unit, RoomDbError>
    fun observeMetadataUploadStatusesForSession(sessionId: UUID): Flow<List<MetadataUploadStepStatus>>
}
