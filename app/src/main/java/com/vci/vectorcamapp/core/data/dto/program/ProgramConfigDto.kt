package com.vci.vectorcamapp.core.data.dto.program

import kotlinx.serialization.Serializable

@Serializable
data class ProgramConfigDto(
    val collectorTitles: List<String>? = null,
    val specimenId: SpecimenIdConfigDto? = null,
)
