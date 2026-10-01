package com.vci.vectorcamapp.core.data.dto.program_model

import kotlinx.serialization.Serializable

@Serializable
data class GetProgramModelsResponseDto(
    val models: List<ProgramModelDto> = emptyList(),
)
