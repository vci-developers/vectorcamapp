package com.vci.vectorcamapp.core.data.dto.program

import kotlinx.serialization.Serializable

@Serializable
data class SpecimenIdConfigDto(
    val validation: String? = null,
    val errorMessage: String? = null,
)
