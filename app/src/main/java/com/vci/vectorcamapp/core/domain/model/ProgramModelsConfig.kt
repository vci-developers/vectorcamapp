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

    companion object {
        /**
         * Fallback when program `config.models` is unset: map roles from conventional
         * modelIds (e.g. uploaded as `species`, `sex`, `abdomen_status`, `detect`).
         */
        fun inferFromModelIds(modelIds: Collection<String>): ProgramModelsConfig {
            val ids = modelIds.map { it.trim() }.filter { it.isNotEmpty() }

            fun pick(vararg candidates: String): String? =
                ids.firstOrNull { id -> candidates.any { id.equals(it, ignoreCase = true) } }

            return ProgramModelsConfig(
                species = pick("species"),
                sex = pick("sex"),
                abdomenStatus = pick("abdomen_status", "abdomenStatus"),
                detect = pick("detect"),
            )
        }
    }
}
