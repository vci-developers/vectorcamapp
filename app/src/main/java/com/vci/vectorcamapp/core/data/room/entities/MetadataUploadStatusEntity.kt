package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import java.util.UUID

@Entity(
    tableName = "metadata_upload_status",
    primaryKeys = ["sessionId", "step"],
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["localId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class MetadataUploadStatusEntity(
    val sessionId: UUID = UUID(0, 0),
    val step: MetadataUploadStep = MetadataUploadStep.SESSION,
    val status: UploadStatus = UploadStatus.NOT_STARTED,
    val updatedAt: Long = 0L,
)
