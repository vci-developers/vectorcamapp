package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.ProgramModel
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError

interface ProgramModelRepository {
    /**
     * Ensures the active program model is cached locally.
     * Returns [Result.Success] with null when the program has no model (404).
     *
     * [onProgress] always reports determinate totals from remote `fileSize`.
     */
    suspend fun syncCurrentModel(
        programId: Int,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): Result<ProgramModel?, NetworkError>

    suspend fun getLocalModel(programId: Int): ProgramModel?
}
