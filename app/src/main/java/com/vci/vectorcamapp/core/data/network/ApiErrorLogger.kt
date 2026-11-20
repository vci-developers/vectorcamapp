package com.vci.vectorcamapp.core.data.network

import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.logging.crashlytics.Severity
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlytics
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlyticsContext
import io.ktor.http.Url
import timber.log.Timber

internal object ApiErrorLogger {
    private const val BODY_LIMIT = 2_000

    fun record(
        method: String,
        route: String,
        httpCode: Int?,
        responseBody: String,
        networkError: NetworkError,
        sessionId: String? = null,
    ) {
        val response = truncate(responseBody.ifBlank { "(empty)" })
        val message = message(method, route, httpCode, response)
        Timber.e(message)
        VectorCamCrashlytics.exception(
            throwable = ApiCallException(message),
            severity = Severity.ERROR,
            context = VectorCamCrashlyticsContext(
                feature = "Api",
                action = "$method $route",
                sessionId = sessionId,
            ),
            tags = mapOf(
                "error_type" to "api_error",
                "http_method" to method,
            ),
            extras = mapOf(
                "api_route" to route,
                "http_code" to (httpCode?.toString() ?: "none"),
                "api_response" to response,
                "network_error" to networkError.name,
            ),
        )
    }

    fun message(method: String, route: String, httpCode: Int?, responseBody: String): String {
        return "$method $route http=${httpCode?.toString() ?: "none"} response=$responseBody"
    }

    fun route(url: Url): String {
        val path = url.encodedPath.ifBlank { "/" }
        val query = url.encodedQuery
        return if (query.isEmpty()) path else "$path?$query"
    }

    private fun truncate(value: String): String {
        if (value.length <= BODY_LIMIT) return value
        return value.take(BODY_LIMIT) + "…(truncated, ${value.length} chars)"
    }
}

internal class ApiCallException(message: String) : Exception(message)
