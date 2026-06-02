package com.vci.vectorcamapp.core.data.dto.session_unit

import kotlinx.serialization.Serializable

@Serializable
data class GetSessionUnitsResponseDto(
    val sessionId: Int = -1,
    val units: List<SessionUnitDto> = emptyList()
)
