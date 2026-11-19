package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.MetadataUploadStatusEntity
import com.vci.vectorcamapp.core.domain.model.MetadataUploadStepStatus
import java.util.UUID

fun MetadataUploadStatusEntity.toDomain(): MetadataUploadStepStatus {
    return MetadataUploadStepStatus(
        step = this.step,
        status = this.status,
        updatedAt = this.updatedAt,
    )
}

fun MetadataUploadStepStatus.toEntity(sessionId: UUID): MetadataUploadStatusEntity {
    return MetadataUploadStatusEntity(
        sessionId = sessionId,
        step = this.step,
        status = this.status,
        updatedAt = this.updatedAt,
    )
}
