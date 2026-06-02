package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.room.entities.SpecimenEntity
import com.vci.vectorcamapp.core.domain.model.Specimen
import java.util.UUID

fun SpecimenEntity.toDomain(): Specimen {
    return Specimen(
        id = this.id,
        remoteId = this.remoteId,
        shouldProcessFurther = this.shouldProcessFurther
    )
}

fun Specimen.toEntity(sessionId: UUID, sessionUnitId: UUID?): SpecimenEntity {
    return SpecimenEntity(
        id = this.id,
        sessionId = sessionId,
        sessionUnitId = sessionUnitId,
        remoteId = this.remoteId,
        shouldProcessFurther = this.shouldProcessFurther
    )
}
