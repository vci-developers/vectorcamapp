package com.vci.vectorcamapp.core.data.dto.session_unit

import kotlinx.serialization.Serializable

@Serializable
data class PostSessionUnitResponseDto(
    val message: String = "",
    val unit: SessionUnitDto = SessionUnitDto(),
)
