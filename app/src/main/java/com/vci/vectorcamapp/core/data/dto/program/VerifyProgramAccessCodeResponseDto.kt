package com.vci.vectorcamapp.core.data.dto.program

import kotlinx.serialization.Serializable

@Serializable
data class VerifyProgramAccessCodeResponseDto(
    val valid: Boolean = false,
)
