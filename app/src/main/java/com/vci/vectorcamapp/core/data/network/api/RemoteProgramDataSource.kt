package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.program.GetAllProgramsResponseDto
import com.vci.vectorcamapp.core.data.dto.program.VerifyAccessCodeRequestDto
import com.vci.vectorcamapp.core.data.dto.program.VerifyAccessCodeResponseDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.network.api.ProgramDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import javax.inject.Inject

class RemoteProgramDataSource @Inject constructor(
    private val httpClient: HttpClient
) : ProgramDataSource {
    override suspend fun getAllPrograms(): Result<GetAllProgramsResponseDto, NetworkError> {
        return safeCall<GetAllProgramsResponseDto> {
            httpClient.get(constructUrl("programs"))
        }
    }

    override suspend fun verifyAccessCode(
        programId: Int,
        accessCode: String,
    ): Result<VerifyAccessCodeResponseDto, NetworkError> {
        return safeCall<VerifyAccessCodeResponseDto> {
            httpClient.post(constructUrl("programs/$programId/verify-access-code")) {
                setBody(VerifyAccessCodeRequestDto(accessCode = accessCode))
            }
        }
    }
}
