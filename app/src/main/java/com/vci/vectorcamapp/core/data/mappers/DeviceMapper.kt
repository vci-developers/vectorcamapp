package com.vci.vectorcamapp.core.data.mappers

import com.vci.vectorcamapp.core.data.dto.cache.DeviceCacheDto
import com.vci.vectorcamapp.core.domain.model.Device

fun DeviceCacheDto.toDomain(): Device? {
    return if (programId == -1) {
        null
    } else {
        Device(
            id = id,
            model = model,
            registeredAt = registeredAt,
            submittedAt = submittedAt
        )
    }
}

fun Device.toDto(programId: Int): DeviceCacheDto {
    return DeviceCacheDto(
        id = id,
        programId = programId,
        model = model,
        registeredAt = registeredAt,
        submittedAt = submittedAt
    )
}
