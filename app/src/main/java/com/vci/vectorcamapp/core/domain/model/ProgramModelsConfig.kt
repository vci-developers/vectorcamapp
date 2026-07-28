package com.vci.vectorcamapp.core.domain.model

/**
 * Maps inference roles to remote [modelId] values from program config.
 * Missing roles fall back to bundled asset models at inference time.
 */
data class ProgramModelsConfig(
    val species: String? = null,
    val sex: String? = null,
    val abdomenStatus: String? = null,
    val detect: String? = null,
) {
    fun modelIds(): List<String> {
        return listOfNotNull(species, sex, abdomenStatus, detect)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun isEmpty(): Boolean = modelIds().isEmpty()
}
