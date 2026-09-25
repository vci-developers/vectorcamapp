package com.vci.vectorcamapp.core.data.dto.program

import kotlinx.serialization.Serializable

@Serializable
data class ProgramConfigDto(
    val models: ProgramModelsConfigDto? = null,
)

@Serializable
data class ProgramModelsConfigDto(
    val species: String? = null,
    val sex: String? = null,
    val abdomenStatus: String? = null,
    val detect: String? = null,
) {
    /** Unique modelIds referenced by inference roles. */
    fun modelIds(): List<String> {
        return listOfNotNull(species, sex, abdomenStatus, detect)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
}
