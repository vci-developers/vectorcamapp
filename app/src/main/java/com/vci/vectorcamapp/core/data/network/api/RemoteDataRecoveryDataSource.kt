package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryRequestDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.network.api.DataRecoveryDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import javax.inject.Inject

class RemoteDataRecoveryDataSource @Inject constructor(
    private val httpClient: HttpClient
) : DataRecoveryDataSource {

    override suspend fun postDataRecovery(
        request: PostDataRecoveryRequestDto
    ): Result<Unit, NetworkError> {
        return safeCall<Unit> {
            httpClient.post(constructUrl(UploadRoutes.DATA_RECOVERIES)) {
                setBody(request)
            }
        }
    }
}
