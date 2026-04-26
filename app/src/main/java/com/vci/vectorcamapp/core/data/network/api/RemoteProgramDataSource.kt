package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.program.GetAllProgramsResponseDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.network.api.ProgramDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import javax.inject.Inject

class RemoteProgramDataSource @Inject constructor(
    private val httpClient: HttpClient
) : ProgramDataSource {
    override suspend fun getAllPrograms(): Result<GetAllProgramsResponseDto, NetworkError> {
        return safeCall<GetAllProgramsResponseDto> {
            httpClient.get(constructUrl("programs"))
        }
    }
}
