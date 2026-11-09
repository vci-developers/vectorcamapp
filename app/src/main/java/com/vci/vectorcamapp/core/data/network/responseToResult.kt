package com.vci.vectorcamapp.core.data.network

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import timber.log.Timber

internal const val DATA_RECOVERY_LOG_TAG = "DataRecovery"

suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, NetworkError> {
    val status = response.status.value
    val url = response.request.url
    Timber.tag(DATA_RECOVERY_LOG_TAG).i("HTTP $status $url")
    return when (status) {
        in 200..299 -> {
            try {
                Result.Success(response.body<T>())
            } catch (e: NoTransformationFoundException) {
                Timber.tag(DATA_RECOVERY_LOG_TAG).e(e, "HTTP $status $url serialization failed")
                Result.Error(NetworkError.SERIALIZATION_ERROR)
            }
        }

        404 -> Result.Error(NetworkError.NOT_FOUND)
        408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
        409 -> Result.Error(NetworkError.CONFLICT)
        429 -> Result.Error(NetworkError.TOO_MANY_REQUESTS)
        in 400..499 -> {
            val body = runCatching { response.bodyAsText() }.getOrNull()
            Timber.tag(DATA_RECOVERY_LOG_TAG).e("HTTP $status $url body=$body")
            Result.Error(NetworkError.CLIENT_ERROR)
        }
        in 500..599 -> {
            val body = runCatching { response.bodyAsText() }.getOrNull()
            Timber.tag(DATA_RECOVERY_LOG_TAG).e("HTTP $status $url body=$body")
            Result.Error(NetworkError.SERVER_ERROR)
        }
        else -> Result.Error(NetworkError.UNKNOWN_ERROR)
    }
}
