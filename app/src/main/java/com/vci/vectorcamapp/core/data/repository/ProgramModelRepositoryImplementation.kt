package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.program_model.LocalProgramModelStore
import com.vci.vectorcamapp.core.domain.model.ProgramModel
import com.vci.vectorcamapp.core.domain.network.api.ProgramModelDataSource
import com.vci.vectorcamapp.core.domain.repository.ProgramModelRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.logging.ProgramModelLog
import javax.inject.Inject

class ProgramModelRepositoryImplementation @Inject constructor(
    private val programModelDataSource: ProgramModelDataSource,
    private val localProgramModelStore: LocalProgramModelStore,
) : ProgramModelRepository {

    override suspend fun syncCurrentModel(
        programId: Int,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<ProgramModel?, NetworkError> {
        ProgramModelLog.i("SYNC start programId=%d", programId)

        when (val metadataResult = programModelDataSource.getCurrentModel(programId)) {
            is Result.Error -> {
                return if (metadataResult.error == NetworkError.NOT_FOUND) {
                    ProgramModelLog.i(
                        "SYNC success with no remote model (404) programId=%d — using bundled assets",
                        programId
                    )
                    Result.Success(null)
                } else {
                    ProgramModelLog.w(
                        "SYNC FAIL metadata error=%s programId=%d",
                        metadataResult.error,
                        programId
                    )
                    Result.Error(metadataResult.error)
                }
            }

            is Result.Success -> {
                val metadata = metadataResult.data

                if (localProgramModelStore.hasMatchingModel(programId, metadata.fileMd5)) {
                    localProgramModelStore.saveMetadata(metadata)
                    val localPath = localProgramModelStore.modelFile(programId).absolutePath
                    onProgress(metadata.fileSize, metadata.fileSize)
                    ProgramModelLog.i(
                        "SYNC success already cached programId=%d version=%s path=%s md5=%s",
                        programId,
                        metadata.version,
                        localPath,
                        metadata.fileMd5
                    )
                    return Result.Success(metadata.toDomain(localPath))
                }

                val tempFile = localProgramModelStore.tempModelFile(programId)
                if (tempFile.exists() && tempFile.length() > metadata.fileSize) {
                    ProgramModelLog.w(
                        "Temp file oversized (%d > %d); deleting",
                        tempFile.length(),
                        metadata.fileSize
                    )
                    tempFile.delete()
                }

                val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
                onProgress(existingBytes.coerceAtMost(metadata.fileSize), metadata.fileSize)
                ProgramModelLog.i(
                    "SYNC download required programId=%d version=%s resumeFrom=%d total=%d downloadUrl=%s",
                    programId,
                    metadata.version,
                    existingBytes,
                    metadata.fileSize,
                    metadata.downloadUrl
                )

                when (
                    val downloadResult = programModelDataSource.downloadModel(
                        downloadPath = metadata.downloadUrl.ifBlank {
                            "/programs/$programId/models/current/download"
                        },
                        destination = tempFile,
                        expectedSize = metadata.fileSize,
                        onProgress = onProgress,
                    )
                ) {
                    is Result.Error -> {
                        ProgramModelLog.w(
                            "SYNC FAIL download error=%s programId=%d partialBytes=%d",
                            downloadResult.error,
                            programId,
                            if (tempFile.exists()) tempFile.length() else 0L
                        )
                        return if (downloadResult.error == NetworkError.NOT_FOUND) {
                            tempFile.delete()
                            ProgramModelLog.i(
                                "SYNC success with no downloadable model (404) programId=%d — using bundled assets",
                                programId
                            )
                            Result.Success(null)
                        } else {
                            Result.Error(downloadResult.error)
                        }
                    }

                    is Result.Success -> {
                        val promoted = localProgramModelStore.promoteTempFile(
                            programId = programId,
                            expectedMd5 = metadata.fileMd5,
                        )
                        if (!promoted) {
                            ProgramModelLog.e(
                                "SYNC FAIL MD5 mismatch programId=%d expectedMd5=%s tempPath=%s",
                                programId,
                                metadata.fileMd5,
                                tempFile.absolutePath
                            )
                            return Result.Error(NetworkError.UNKNOWN_ERROR)
                        }

                        localProgramModelStore.saveMetadata(metadata)
                        val localPath = localProgramModelStore.modelFile(programId).absolutePath
                        onProgress(metadata.fileSize, metadata.fileSize)
                        ProgramModelLog.i(
                            "SYNC SUCCESS downloaded programId=%d version=%s path=%s md5=%s size=%d",
                            programId,
                            metadata.version,
                            localPath,
                            metadata.fileMd5,
                            metadata.fileSize
                        )
                        return Result.Success(metadata.toDomain(localPath))
                    }
                }
            }
        }
    }

    override suspend fun getLocalModel(programId: Int): ProgramModel? {
        val metadata = localProgramModelStore.getCachedMetadata(programId)
        val modelFile = localProgramModelStore.modelFile(programId)
        if (metadata == null || !modelFile.exists()) {
            ProgramModelLog.d("Local model missing programId=%d", programId)
            return null
        }
        ProgramModelLog.d(
            "Local model found programId=%d version=%s path=%s",
            programId,
            metadata.version,
            modelFile.absolutePath
        )
        return metadata.toDomain(modelFile.absolutePath)
    }
}
