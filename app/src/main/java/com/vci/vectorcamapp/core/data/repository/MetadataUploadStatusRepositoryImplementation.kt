package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.MetadataUploadStatusDao
import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import com.vci.vectorcamapp.core.domain.repository.MetadataUploadStatusRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class MetadataUploadStatusRepositoryImplementation @Inject constructor(
    private val metadataUploadStatusDao: MetadataUploadStatusDao
) : MetadataUploadStatusRepository {

    override suspend fun upsertMetadataUploadStatus(
        metadataUploadStepStatus: MetadataUploadStepStatus,
        sessionId: UUID,
    ): Result<Unit, RoomDbError> {
        return try {
            metadataUploadStatusDao.upsertMetadataUploadStatus(
                metadataUploadStepStatus.toEntity(sessionId)
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun failInProgressMetadataUploadStatuses(
        sessionId: UUID
    ): Result<Unit, RoomDbError> {
        return try {
            metadataUploadStatusDao.failInProgressMetadataUploadStatuses(
                sessionId, System.currentTimeMillis()
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override fun observeMetadataUploadStatusesForSession(
        sessionId: UUID
    ): Flow<List<MetadataUploadStepStatus>> {
        return metadataUploadStatusDao.observeMetadataUploadStatusesForSession(sessionId)
            .map { entities -> entities.map { it.toDomain() } }
    }
}
