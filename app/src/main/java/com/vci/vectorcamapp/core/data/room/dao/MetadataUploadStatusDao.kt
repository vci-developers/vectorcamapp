package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.MetadataUploadStatusEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface MetadataUploadStatusDao {

    @Upsert
    suspend fun upsertMetadataUploadStatus(metadataUploadStatus: MetadataUploadStatusEntity)

    @Query(
        "UPDATE metadata_upload_status SET status = 'FAILED', updatedAt = :updatedAt " +
                "WHERE sessionId = :sessionId AND status = 'IN_PROGRESS'"
    )
    suspend fun failInProgressMetadataUploadStatuses(sessionId: UUID, updatedAt: Long)

    @Query("SELECT * FROM metadata_upload_status WHERE sessionId = :sessionId")
    fun observeMetadataUploadStatusesForSession(sessionId: UUID): Flow<List<MetadataUploadStatusEntity>>
}
