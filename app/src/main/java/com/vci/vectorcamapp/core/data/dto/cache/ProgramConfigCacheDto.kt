package com.vci.vectorcamapp.core.data.dto.cache

import com.vci.vectorcamapp.core.data.dto.program.SpecimenIdConfigDto
import kotlinx.serialization.Serializable

@Serializable
data class ProgramConfigCacheDto(
    val collectorTitles: List<String>? = null,
    val specimenId: SpecimenIdConfigDto? = null,
)
