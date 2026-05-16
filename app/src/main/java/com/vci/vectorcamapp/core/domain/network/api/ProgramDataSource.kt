package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.program.GetAllProgramsResponseDto
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError

interface ProgramDataSource {
    suspend fun getAllPrograms(): Result<GetAllProgramsResponseDto, NetworkError>

    suspend fun verifyAccessCode(
        programId: Int,
        accessCode: String,
    ): VerifyAccessCodeResult
}
