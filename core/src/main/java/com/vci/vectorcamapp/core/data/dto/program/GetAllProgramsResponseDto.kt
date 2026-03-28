package com.vci.vectorcamapp.core.data.dto.program

import kotlinx.serialization.Serializable

@Serializable
data class GetAllProgramsResponseDto(
    val programs: List<ProgramDto>
)
