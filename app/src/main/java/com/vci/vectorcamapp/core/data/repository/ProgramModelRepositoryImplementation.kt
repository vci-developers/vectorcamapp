package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.dto.program_model.ProgramModelDto
import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toModelsConfig
import com.vci.vectorcamapp.core.data.program_model.LocalProgramModelStore
import com.vci.vectorcamapp.core.domain.model.ProgramModel
import com.vci.vectorcamapp.core.domain.model.ProgramModelsConfig
import com.vci.vectorcamapp.core.domain.network.api.ProgramDataSource
import com.vci.vectorcamapp.core.domain.network.api.ProgramModelDataSource
import com.vci.vectorcamapp.core.domain.repository.InferenceModelRole
import com.vci.vectorcamapp.core.domain.repository.ProgramModelRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.logging.ProgramModelLog
import javax.inject.Inject

class ProgramModelRepositoryImplementation @Inject constructor(
    private val programDataSource: ProgramDataSource,
    private val programModelDataSource: ProgramModelDataSource,
    private val localProgramModelStore: LocalProgramModelStore,
) : ProgramModelRepository {

    override suspend fun syncConfiguredModels(
        programId: Int,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<List<ProgramModel>, NetworkError> {
        ProgramModelLog.i("SYNC start programId=%d", programId)

        // Program config is optional (maps roles -> modelId). Never let it block downloading
        // the models the list endpoint actually returns.
        val apiConfig = when (val programResult = programDataSource.getProgramById(programId)) {
            is Result.Error -> {
                ProgramModelLog.w(
                    "SYNC WARN get program error=%s programId=%d — continuing without API config",
                    programResult.error,
                    programId
                )
                null
            }

            is Result.Success -> {
                val modelsConfig = programResult.data.config?.toModelsConfig()
                ProgramModelLog.i(
                    "SYNC program config programId=%d models=%s",
                    programId,
                    modelsConfig?.modelIds()?.joinToString().orEmpty().ifEmpty { "(none)" }
                )
                modelsConfig
            }
        }

        val listedModels = when (val listResult = programModelDataSource.getModels(programId)) {
            is Result.Error -> {
                if (listResult.error == NetworkError.NOT_FOUND) {
                    ProgramModelLog.i(
                        "SYNC models list 404 programId=%d — using bundled assets",
                        programId
                    )
                    onProgress(0L, 0L)
                    return Result.Success(emptyList())
                }
                ProgramModelLog.w(
                    "SYNC FAIL models list error=%s programId=%d",
                    listResult.error,
                    programId
                )
                return Result.Error(listResult.error)
            }

            is Result.Success -> {
                ProgramModelLog.i(
                    "SYNC models list SUCCESS programId=%d count=%d modelIds=%s",
                    programId,
                    listResult.data.models.size,
                    listResult.data.models.joinToString { it.modelId }
                )
                listResult.data.models
            }
        }

        val metadataById = linkedMapOf<String, ProgramModelDto>()
        for (model in listedModels) {
            metadataById[model.modelId] = model
        }

        if (metadataById.isEmpty()) {
            ProgramModelLog.i(
                "SYNC success no downloadable configured models programId=%d — using bundled assets",
                programId
            )
            onProgress(0L, 0L)
            return Result.Success(emptyList())
        }

        val totalBytes = metadataById.values.sumOf { it.fileSize.coerceAtLeast(0L) }
        var completedBytes = 0L
        onProgress(0L, totalBytes)

        val synced = mutableListOf<ProgramModel>()

        for ((modelId, metadata) in metadataById) {
            ProgramModelLog.i(
                "SYNC model begin modelId=%s size=%d md5=%s",
                modelId,
                metadata.fileSize,
                metadata.fileMd5
            )

            if (localProgramModelStore.hasMatchingModel(programId, modelId, metadata.fileMd5)) {
                localProgramModelStore.saveMetadata(metadata)
                val path = localProgramModelStore.modelFile(programId, modelId).absolutePath
                completedBytes += metadata.fileSize
                onProgress(completedBytes.coerceAtMost(totalBytes), totalBytes)
                ProgramModelLog.i(
                    "SYNC model already cached modelId=%s path=%s",
                    modelId,
                    path
                )
                synced += metadata.toDomain(path)
                continue
            }

            val tempFile = localProgramModelStore.tempModelFile(programId, modelId)
            if (tempFile.exists() && tempFile.length() > metadata.fileSize) {
                ProgramModelLog.w(
                    "Temp file oversized for modelId=%s (%d > %d); deleting",
                    modelId,
                    tempFile.length(),
                    metadata.fileSize
                )
                tempFile.delete()
            }

            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
            onProgress(
                (completedBytes + existingBytes).coerceAtMost(totalBytes),
                totalBytes
            )

            when (
                val downloadResult = programModelDataSource.downloadModel(
                    downloadPath = metadata.downloadUrl.ifBlank {
                        "/programs/$programId/models/$modelId/download"
                    },
                    destination = tempFile,
                    expectedSize = metadata.fileSize,
                    onProgress = { bytesDownloaded, _ ->
                        onProgress(
                            (completedBytes + bytesDownloaded).coerceAtMost(totalBytes),
                            totalBytes
                        )
                    },
                )
            ) {
                is Result.Error -> {
                    ProgramModelLog.w(
                        "SYNC FAIL download modelId=%s error=%s partialBytes=%d",
                        modelId,
                        downloadResult.error,
                        if (tempFile.exists()) tempFile.length() else 0L
                    )
                    if (downloadResult.error == NetworkError.NOT_FOUND) {
                        ProgramModelLog.w("SYNC modelId=%s download 404 — skipping", modelId)
                        continue
                    }
                    return Result.Error(downloadResult.error)
                }

                is Result.Success -> {
                    val promoted = localProgramModelStore.promoteTempFile(
                        programId = programId,
                        modelId = modelId,
                        expectedMd5 = metadata.fileMd5,
                    )
                    if (!promoted) {
                        ProgramModelLog.e(
                            "SYNC FAIL MD5 mismatch modelId=%s expectedMd5=%s",
                            modelId,
                            metadata.fileMd5
                        )
                        return Result.Error(NetworkError.UNKNOWN_ERROR)
                    }

                    localProgramModelStore.saveMetadata(metadata)
                    val path = localProgramModelStore.modelFile(programId, modelId).absolutePath
                    completedBytes += metadata.fileSize
                    onProgress(completedBytes.coerceAtMost(totalBytes), totalBytes)
                    ProgramModelLog.i(
                        "SYNC model SUCCESS modelId=%s path=%s md5=%s",
                        modelId,
                        path,
                        metadata.fileMd5
                    )
                    synced += metadata.toDomain(path)
                }
            }
        }

        ProgramModelLog.i(
            "SYNC SUCCESS programId=%d downloaded=%d modelIds=%s",
            programId,
            synced.size,
            synced.joinToString { it.modelId }
        )

        // Persist role -> modelId mapping to disk (config.json) so Imaging can find downloaded
        // models later without re-syncing. Prefer the API-provided config; fall back to
        // inferring roles from conventional modelId names among the models actually downloaded.
        val effectiveConfig = apiConfig?.takeUnless { it.isEmpty() }
            ?: ProgramModelsConfig.inferFromModelIds(synced.map { it.modelId })

        if (!effectiveConfig.isEmpty()) {
            localProgramModelStore.saveConfig(programId, effectiveConfig)
            ProgramModelLog.i(
                "SYNC saved config.json programId=%d source=%s models=%s",
                programId,
                if (apiConfig?.isEmpty() == false) "api" else "inferred",
                effectiveConfig.modelIds().joinToString()
            )
        } else {
            ProgramModelLog.w(
                "SYNC no role mapping resolved programId=%d (no API config, no conventional modelIds among %s) — Imaging will use bundled assets",
                programId,
                synced.joinToString { it.modelId }.ifEmpty { "none" }
            )
        }

        return Result.Success(synced)
    }

    override suspend fun getLocalModel(programId: Int, modelId: String): ProgramModel? {
        val metadata = localProgramModelStore.getCachedMetadata(programId, modelId) ?: return null
        val modelFile = localProgramModelStore.modelFile(programId, modelId)
        if (!modelFile.exists()) return null
        return metadata.toDomain(modelFile.absolutePath)
    }

    override suspend fun getLocalModelsConfig(programId: Int): ProgramModelsConfig? {
        return localProgramModelStore.getCachedConfig(programId)
    }

    override suspend fun getLocalModelPathForRole(
        programId: Int,
        role: InferenceModelRole,
    ): String? {
        val config = localProgramModelStore.getCachedConfigSync(programId) ?: return null
        val modelId = when (role) {
            InferenceModelRole.SPECIES -> config.species
            InferenceModelRole.SEX -> config.sex
            InferenceModelRole.ABDOMEN_STATUS -> config.abdomenStatus
            InferenceModelRole.DETECT -> config.detect
        }?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        return localProgramModelStore.modelPathIfExists(programId, modelId)
    }
}
