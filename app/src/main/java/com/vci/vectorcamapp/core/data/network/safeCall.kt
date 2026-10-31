package com.vci.vectorcamapp.core.data.network

import com.vci.vectorcamapp.core.data.upload.logging.UploadFailureReport
import com.vci.vectorcamapp.core.data.upload.logging.UploadHttpExchange
import com.vci.vectorcamapp.core.data.upload.logging.UploadTrace
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, NetworkError> {
    clearUploadExchange()
    val response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        e.printStackTrace()
        rememberTransportFailure(e)
        return Result.Error(NetworkError.NO_INTERNET)
    } catch (e: UnknownHostException) {
        e.printStackTrace()
        rememberTransportFailure(e)
        return Result.Error(NetworkError.NO_INTERNET)
    } catch (e: SerializationException) {
        e.printStackTrace()
        rememberTransportFailure(e)
        return Result.Error(NetworkError.SERIALIZATION_ERROR)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        e.printStackTrace()
        rememberTransportFailure(e)
        return Result.Error(NetworkError.UNKNOWN_ERROR)
    }

    val result = responseToResult<T>(response)
    if (result is Result.Error) {
        rememberHttpFailure(response, result.error)
    }
    return result
}

@PublishedApi
internal suspend fun clearUploadExchange() {
    coroutineContext[UploadTrace]?.lastExchange = null
}

@PublishedApi
internal suspend fun rememberTransportFailure(error: Throwable) {
    val trace = coroutineContext[UploadTrace] ?: return
    trace.failureCause = error
    trace.lastExchange = UploadHttpExchange(
        method = "UNKNOWN",
        endpoint = "(request failed before a response)",
        requestBody = "(unavailable)",
        statusCode = null,
        responseBody = UploadFailureReport.truncate(error.stackTraceToString()),
    )
}

@PublishedApi
internal suspend fun rememberHttpFailure(response: HttpResponse, error: NetworkError) {
    val trace = coroutineContext[UploadTrace] ?: return
    val requestBody = try {
        UploadFailureReport.outgoingContent(response.call.request.content)
    } catch (expected: CancellationException) {
        throw expected
    } catch (expected: Exception) {
        "(unreadable request body: ${expected.message})"
    }
    val responseBody = try {
        response.bodyAsText().ifBlank { "(empty)" }
    } catch (expected: CancellationException) {
        throw expected
    } catch (expected: Exception) {
        "(unreadable response body: ${expected.message})"
    }
    trace.lastExchange = UploadHttpExchange(
        method = response.call.request.method.value,
        endpoint = response.call.request.url.toString(),
        requestBody = UploadFailureReport.truncate(requestBody),
        statusCode = response.status.value,
        responseBody = UploadFailureReport.truncate(
            responseBody + " networkError=${error.name}",
        ),
    )
}
