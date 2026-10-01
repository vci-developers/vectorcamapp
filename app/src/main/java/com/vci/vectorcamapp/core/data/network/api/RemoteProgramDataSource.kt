package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.program.GetAllProgramsResponseDto
import com.vci.vectorcamapp.core.data.dto.program.ProgramDto
import com.vci.vectorcamapp.core.data.dto.program.VerifyProgramAccessCodeRequestDto
import com.vci.vectorcamapp.core.data.dto.program.VerifyProgramAccessCodeResponseDto
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
        return safeCall {
            httpClient.get(constructUrl("programs"))
        }
    }

    override suspend fun getProgramById(programId: Int): Result<ProgramDto, NetworkError> {
        return safeCall {
            httpClient.get(constructUrl("programs/$programId"))
        }
    }

    override suspend fun verifyAccessCode(
        programId: Int,
        accessCode: String,
    ): Result<VerifyProgramAccessCodeResponseDto, NetworkError> {
        return safeCall {
            httpClient.post(constructUrl("programs/$programId/verify-access-code")) {
                setBody(VerifyProgramAccessCodeRequestDto(accessCode = accessCode))
            }
        }
    }
}
