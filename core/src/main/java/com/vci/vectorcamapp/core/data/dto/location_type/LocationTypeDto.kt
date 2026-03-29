package com.vci.vectorcamapp.core.data.dto.location_type

import kotlinx.serialization.Serializable

@Serializable
data class LocationTypeDto(
    val id: Int = -1,
    val programId: Int = -1,
    val name: String = "",
    val level: Int = -1,
)
