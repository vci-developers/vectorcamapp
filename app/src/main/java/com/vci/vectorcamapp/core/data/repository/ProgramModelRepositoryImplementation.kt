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

        val apiConfig = resolveOptionalApiConfig(programId)
        val listedModels = when (val listResult = loadListedModels(programId, onProgress)) {
            is Result.Error -> return listResult
            is Result.Success -> listResult.data
        }
        if (listedModels.isEmpty()) {
            return Result.Success(emptyList())
        }

        val syncedResult = downloadListedModels(programId, listedModels, onProgress)
        if (syncedResult is Result.Error) {
            return syncedResult
        }
        val synced = (syncedResult as Result.Success).data

        persistRoleMapping(programId, apiConfig, synced)
        ProgramModelLog.i(
            "SYNC SUCCESS programId=%d downloaded=%d modelIds=%s",
            programId,
            synced.size,
            synced.joinToString { it.modelId }
        )
        return Result.Success(synced)
    }

    private suspend fun resolveOptionalApiConfig(programId: Int): ProgramModelsConfig? {
        return when (val programResult = programDataSource.getProgramById(programId)) {
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
    }

    private suspend fun loadListedModels(
        programId: Int,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<List<ProgramModelDto>, NetworkError> {
        return when (val listResult = programModelDataSource.getModels(programId)) {
            is Result.Error -> {
                if (listResult.error == NetworkError.NOT_FOUND) {
                    ProgramModelLog.i(
                        "SYNC models list 404 programId=%d — using bundled assets",
                        programId
                    )
                    onProgress(0L, 0L)
                    Result.Success(emptyList())
                } else {
                    ProgramModelLog.w(
                        "SYNC FAIL models list error=%s programId=%d",
                        listResult.error,
                        programId
                    )
                    Result.Error(listResult.error)
                }
            }

            is Result.Success -> {
                ProgramModelLog.i(
                    "SYNC models list SUCCESS programId=%d count=%d modelIds=%s",
                    programId,
                    listResult.data.models.size,
                    listResult.data.models.joinToString { it.modelId }
                )
                if (listResult.data.models.isEmpty()) {
                    ProgramModelLog.i(
                        "SYNC success no downloadable configured models programId=%d — using bundled assets",
                        programId
                    )
                    onProgress(0L, 0L)
                }
                Result.Success(listResult.data.models)
            }
        }
    }

    private suspend fun downloadListedModels(
        programId: Int,
        listedModels: List<ProgramModelDto>,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<List<ProgramModel>, NetworkError> {
        val metadataById = linkedMapOf<String, ProgramModelDto>()
        for (model in listedModels) {
            metadataById[model.modelId] = model
        }

        val totalBytes = metadataById.values.sumOf { it.fileSize.coerceAtLeast(0L) }
        var completedBytes = 0L
        onProgress(0L, totalBytes)

        val synced = mutableListOf<ProgramModel>()
        for ((_, metadata) in metadataById) {
            when (
                val modelResult = syncSingleModel(
                    programId = programId,
                    metadata = metadata,
                    completedBytes = completedBytes,
                    totalBytes = totalBytes,
                    onProgress = onProgress,
                )
            ) {
                is Result.Error -> return modelResult
                is Result.Success -> {
                    val model = modelResult.data
                    if (model != null) {
                        completedBytes += metadata.fileSize
                        synced += model
                    }
                }
            }
        }
        return Result.Success(synced)
    }

    private suspend fun syncSingleModel(
        programId: Int,
        metadata: ProgramModelDto,
        completedBytes: Long,
        totalBytes: Long,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<ProgramModel?, NetworkError> {
        val modelId = metadata.modelId
        ProgramModelLog.i(
            "SYNC model begin modelId=%s size=%d md5=%s",
            modelId,
            metadata.fileSize,
            metadata.fileMd5
        )

        if (localProgramModelStore.hasMatchingModel(programId, modelId, metadata.fileMd5)) {
            localProgramModelStore.saveMetadata(metadata)
            val path = localProgramModelStore.modelFile(programId, modelId).absolutePath
            onProgress((completedBytes + metadata.fileSize).coerceAtMost(totalBytes), totalBytes)
            ProgramModelLog.i("SYNC model already cached modelId=%s path=%s", modelId, path)
            return Result.Success(metadata.toDomain(path))
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
        onProgress((completedBytes + existingBytes).coerceAtMost(totalBytes), totalBytes)

        return when (
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
                    Result.Success(null)
                } else {
                    Result.Error(downloadResult.error)
                }
            }

            is Result.Success -> promoteDownloadedModel(
                programId = programId,
                metadata = metadata,
                completedBytes = completedBytes,
                totalBytes = totalBytes,
                onProgress = onProgress,
            )
        }
    }

    private suspend fun promoteDownloadedModel(
        programId: Int,
        metadata: ProgramModelDto,
        completedBytes: Long,
        totalBytes: Long,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<ProgramModel?, NetworkError> {
        val modelId = metadata.modelId
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
        onProgress((completedBytes + metadata.fileSize).coerceAtMost(totalBytes), totalBytes)
        ProgramModelLog.i(
            "SYNC model SUCCESS modelId=%s path=%s md5=%s",
            modelId,
            path,
            metadata.fileMd5
        )
        return Result.Success(metadata.toDomain(path))
    }

    private suspend fun persistRoleMapping(
        programId: Int,
        apiConfig: ProgramModelsConfig?,
        synced: List<ProgramModel>,
    ) {
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
