package com.vci.vectorcamapp.core.data.upload.logging

import io.ktor.http.content.OutgoingContent
import java.net.HttpURLConnection

internal object UploadFailureReport {
    const val BODY_LIMIT = 2_000
    private const val MESSAGE_LIMIT = 8_000

    fun build(
        worker: String,
        sessionId: String?,
        step: String?,
        programId: String?,
        siteId: String?,
        specimenId: String?,
        imageId: String?,
        reason: String,
        networkError: String?,
        exchange: UploadHttpExchange?,
    ): String {
        val requestBody = exchange?.requestBody ?: "(no http call)"
        val responseBody = exchange?.responseBody ?: "(no http response)"
        return truncate(
            buildString {
                append(worker)
                append(" failed")
                append(" session=").append(sessionId ?: "unknown")
                append(" step=").append(step ?: "unknown")
                programId?.let { append(" program=").append(it) }
                siteId?.let { append(" site=").append(it) }
                specimenId?.let { append(" specimen=").append(it) }
                imageId?.let { append(" image=").append(it) }
                append(" reason=").append(reason)
                networkError?.let { append(" networkError=").append(it) }
                append(" method=").append(exchange?.method ?: "(none)")
                append(" endpoint=").append(exchange?.endpoint ?: "(none)")
                append(" status=").append(exchange?.statusCode?.toString() ?: "none")
                append(" request=").append(truncate(requestBody))
                append(" response=").append(truncate(responseBody))
            },
            MESSAGE_LIMIT,
        )
    }

    fun exchangeFrom(
        throwable: Throwable,
        connection: HttpURLConnection?,
        fallbackMethod: String,
        fallbackEndpoint: String?,
        requestBody: String?,
    ): UploadHttpExchange {
        return UploadHttpExchange(
            method = connection.requestMethodOrNull() ?: fallbackMethod,
            endpoint = connection.endpointOrNull() ?: fallbackEndpoint ?: "(unknown endpoint)",
            requestBody = truncate(requestBody ?: "(empty)"),
            statusCode = connection.responseCodeOrNull(),
            responseBody = truncate(connection.responseBodyOrNull() ?: throwable.message ?: "(empty)"),
        )
    }

    fun outgoingContent(content: OutgoingContent): String {
        return when (content) {
            is OutgoingContent.ByteArrayContent -> {
                content.bytes().toString(Charsets.UTF_8).ifBlank { "(empty)" }
            }
            is OutgoingContent.NoContent -> "(empty)"
            else -> "(non-text body omitted, type=${content::class.simpleName})"
        }
    }

    fun truncate(value: String, limit: Int = BODY_LIMIT): String {
        if (value.length <= limit) return value
        return value.take(limit) + "…(truncated, ${value.length} chars)"
    }

    private fun HttpURLConnection?.requestMethodOrNull(): String? {
        return try {
            this?.requestMethod
        } catch (expected: Exception) {
            null
        }
    }

    private fun HttpURLConnection?.endpointOrNull(): String? {
        return try {
            this?.url?.toString()
        } catch (expected: Exception) {
            null
        }
    }

    private fun HttpURLConnection?.responseCodeOrNull(): Int? {
        return try {
            this?.responseCode
        } catch (expected: Exception) {
            null
        }
    }

    private fun HttpURLConnection?.responseBodyOrNull(): String? {
        if (this == null) return null
        return try {
            val stream = errorStream ?: return "(empty)"
            stream.bufferedReader().use { it.readText() }.ifBlank { "(empty)" }
        } catch (expected: Exception) {
            "(unreadable response body: ${expected.message})"
        }
    }
}
