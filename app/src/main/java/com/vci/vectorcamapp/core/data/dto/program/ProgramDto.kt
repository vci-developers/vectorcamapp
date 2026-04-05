package com.vci.vectorcamapp.core.data.dto.program

import kotlinx.serialization.Serializable

@Serializable
data class ProgramDto(
    val programId: Int = -1,
    val name: String = "",
    val country: String = "",
    val formVersion: String? = null
)
