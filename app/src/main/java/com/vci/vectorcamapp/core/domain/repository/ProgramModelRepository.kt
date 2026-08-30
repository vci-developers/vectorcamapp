package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.ProgramModel
import com.vci.vectorcamapp.core.domain.model.ProgramModelsConfig
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError

interface ProgramModelRepository {
    /**
     * Fetches program config, then downloads every configured modelId.
     * Returns [Result.Success] with the synced models (empty when none configured).
     *
     * [onProgress] reports aggregate bytes across all models for a determinate UI bar.
     */
    suspend fun syncConfiguredModels(
        programId: Int,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): Result<List<ProgramModel>, NetworkError>

    suspend fun getLocalModel(programId: Int, modelId: String): ProgramModel?

    suspend fun getLocalModelsConfig(programId: Int): ProgramModelsConfig?

    /** Absolute path for a role's downloaded model, or null to use bundled assets. */
    suspend fun getLocalModelPathForRole(programId: Int, role: InferenceModelRole): String?
}

enum class InferenceModelRole {
    SPECIES,
    SEX,
    ABDOMEN_STATUS,
    DETECT,
}
