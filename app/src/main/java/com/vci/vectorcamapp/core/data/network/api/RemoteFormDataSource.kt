package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.form.FormDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.network.api.FormDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import javax.inject.Inject

class RemoteFormDataSource @Inject constructor(
    private val httpClient: HttpClient
) : FormDataSource {
    override suspend fun getCurrentFormByProgramId(programId: Int): Result<FormDto, NetworkError> {
        return safeCall<FormDto> {
            httpClient.get(constructUrl("/programs/$programId/forms/current"))
        }
    }
}