package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.BoundingBoxEntity
import com.vci.vectorcamapp.core.domain.model.BoundingBox

fun BoundingBoxEntity.toDomain() : BoundingBox {
    return BoundingBox(
        topLeftX = this.topLeftX,
        topLeftY = this.topLeftY,
        width = this.width,
        height = this.height,
        confidence = this.confidence,
        classId = this.classId,
    )
}

fun BoundingBox.toEntity(specimenId: String) : BoundingBoxEntity {
    return BoundingBoxEntity(
        specimenId = specimenId,
        topLeftX = this.topLeftX,
        topLeftY = this.topLeftY,
        width = this.width,
        height = this.height,
        confidence = this.confidence,
        classId = this.classId,
    )
}