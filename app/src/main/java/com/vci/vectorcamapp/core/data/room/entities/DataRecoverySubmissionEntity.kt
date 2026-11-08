package com.vci.vectorcamapp.core.data.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vci.vectorcamapp.core.domain.model.enums.MetadataUploadStep
import java.util.UUID

@Entity(
    tableName = "data_recovery_submission",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["localId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class DataRecoverySubmissionEntity(
    @PrimaryKey val idempotencyKey: String = "",
    val sessionId: UUID = UUID(0, 0),
    val step: MetadataUploadStep = MetadataUploadStep.SESSION,
    val dataRecoveryId: Int? = null,
    val submittedAt: Long = 0L,
)
