package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.program_model.GetProgramModelsResponseDto
import com.vci.vectorcamapp.core.data.dto.program_model.ProgramModelDto
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import java.io.File

interface ProgramModelDataSource {
    suspend fun getModels(programId: Int): Result<GetProgramModelsResponseDto, NetworkError>

    suspend fun getModel(programId: Int, modelId: String): Result<ProgramModelDto, NetworkError>

    /**
     * Resolves a presigned S3 URL via the API download endpoint (302 Location),
     * then downloads with HTTP Range resume support.
     *
     * @param downloadPath relative path from [ProgramModelDto.downloadUrl]
     */
    suspend fun downloadModel(
        downloadPath: String,
        destination: File,
        expectedSize: Long,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<Unit, NetworkError>
}
