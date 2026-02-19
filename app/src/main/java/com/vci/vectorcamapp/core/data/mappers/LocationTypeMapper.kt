package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.location_type.LocationTypeDto
import com.vci.vectorcamapp.core.data.room.entities.LocationTypeEntity
import com.vci.vectorcamapp.core.domain.model.LocationType

fun LocationTypeEntity.toDomain(): LocationType {
    return LocationType(
        id = this.id,
        name = this.name
    )
}

fun LocationType.toEntity(programId: Int): LocationTypeEntity {
    return LocationTypeEntity(
        id = this.id,
        name = this.name,
        programId = programId
    )
}

fun LocationTypeDto.toDomain(): LocationType {
    return LocationType(
        id = this.id,
        name = this.name
    )
}
