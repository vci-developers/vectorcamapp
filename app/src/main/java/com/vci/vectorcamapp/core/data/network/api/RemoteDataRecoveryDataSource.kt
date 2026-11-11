package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryRequestDto
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryResponseDto
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostStaleSpecimenImageRecoveryRequestDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.network.api.DataRecoveryDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import timber.log.Timber
import javax.inject.Inject

class RemoteDataRecoveryDataSource @Inject constructor(
    private val httpClient: HttpClient
) : DataRecoveryDataSource {

    override suspend fun postDataRecovery(
        request: PostDataRecoveryRequestDto
    ): Result<PostDataRecoveryResponseDto, NetworkError> {
        Timber.tag(TAG).i(
            "POST ${UploadRoutes.DATA_RECOVERIES} key=${request.idempotencyKey} " +
                "session=${request.sessionFrontendId} route=${request.payload.route} " +
                "error=${request.payload.error}"
        )
        val result = safeCall<PostDataRecoveryResponseDto> {
            httpClient.post(constructUrl(UploadRoutes.DATA_RECOVERIES)) {
                setBody(request)
            }
        }
        when (result) {
            is Result.Success -> Timber.tag(TAG).i(
                "POST ${UploadRoutes.DATA_RECOVERIES} success key=${request.idempotencyKey} " +
                    "recoveryId=${result.data.dataRecovery?.id} duplicate=${result.data.duplicate}"
            )
            is Result.Error -> Timber.tag(TAG).e(
                "POST ${UploadRoutes.DATA_RECOVERIES} error key=${request.idempotencyKey}: ${result.error}"
            )
        }
        return result
    }

    override suspend fun postStaleSpecimenImageRecovery(
        request: PostStaleSpecimenImageRecoveryRequestDto
    ): Result<PostDataRecoveryResponseDto, NetworkError> {
        Timber.tag(TAG).i(
            "POST ${UploadRoutes.DATA_RECOVERIES} key=${request.idempotencyKey} " +
                "session=${request.sessionFrontendId} type=${request.payload.type} " +
                "filemd5=${request.payload.filemd5} clientSpecimenId=${request.payload.clientSpecimenId}"
        )
        val result = safeCall<PostDataRecoveryResponseDto> {
            httpClient.post(constructUrl(UploadRoutes.DATA_RECOVERIES)) {
                setBody(request)
            }
        }
        when (result) {
            is Result.Success -> Timber.tag(TAG).i(
                "POST ${UploadRoutes.DATA_RECOVERIES} image recovery success " +
                    "key=${request.idempotencyKey} recoveryId=${result.data.dataRecovery?.id} " +
                    "duplicate=${result.data.duplicate}"
            )
            is Result.Error -> Timber.tag(TAG).e(
                "POST ${UploadRoutes.DATA_RECOVERIES} image recovery error " +
                    "key=${request.idempotencyKey}: ${result.error}"
            )
        }
        return result
    }

    companion object {
        private const val TAG = "DataRecovery"
    }
}
