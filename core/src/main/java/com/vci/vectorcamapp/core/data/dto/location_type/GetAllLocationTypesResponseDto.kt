package com.vci.vectorcamapp.core.data.dto.location_type

import kotlinx.serialization.Serializable

@Serializable
data class GetAllLocationTypesResponseDto(
    val locationTypes: List<LocationTypeDto>
)
