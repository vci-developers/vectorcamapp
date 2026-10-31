package com.vci.vectorcamapp.core.data.upload.logging

import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.logging.crashlytics.Severity
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlytics
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlyticsContext
import io.tus.java.client.ProtocolException
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection

internal object UploadErrorLogger {
    fun record(
        trace: UploadTrace?,
        reason: String,
        networkError: String? = null,
        throwable: Throwable? = null,
    ) {
        val exchange = trace?.lastExchange
        val cause = throwable ?: trace?.failureCause
        val message = UploadFailureReport.build(
            worker = trace?.worker ?: "Upload",
            sessionId = trace?.sessionId,
            step = trace?.step,
            programId = trace?.programId,
            siteId = trace?.siteId,
            specimenId = trace?.specimenId,
            imageId = trace?.imageId,
            reason = reason,
            networkError = networkError,
            exchange = exchange,
        )
        Timber.e(message)
        VectorCamCrashlytics.exception(
            throwable = UploadFailureException(message, cause),
            severity = Severity.ERROR,
            context = VectorCamCrashlyticsContext(
                screen = "Upload",
                feature = trace?.worker,
                action = trace?.step,
                sessionId = trace?.sessionId,
                programId = trace?.programId,
                siteId = trace?.siteId,
                specimenId = trace?.specimenId,
            ),
            tags = mapOf(
                "error_type" to "upload_failed",
                "upload_worker" to (trace?.worker ?: "Upload"),
            ),
            extras = mapOf(
                "reason" to reason,
                "network_error" to networkError,
                "image_id" to trace?.imageId,
                "http_method" to (exchange?.method ?: "(none)"),
                "http_endpoint" to (exchange?.endpoint ?: "(none)"),
                "http_status" to (exchange?.statusCode?.toString() ?: "none"),
                "request_body" to (exchange?.requestBody ?: "(no http call)"),
                "response_body" to (exchange?.responseBody ?: "(no http response)"),
            ),
        )
        trace?.lastExchange = null
        trace?.failureCause = null
    }

    fun rememberTusFailure(
        trace: UploadTrace?,
        throwable: Throwable,
        connection: HttpURLConnection?,
        fallbackMethod: String,
    ) {
        if (trace == null) return
        trace.failureCause = throwable
        trace.lastExchange = UploadFailureReport.exchangeFrom(
            throwable = throwable,
            connection = connection,
            fallbackMethod = fallbackMethod,
            fallbackEndpoint = trace.endpointHint,
            requestBody = trace.requestBodyHint,
        )
        val exchange = trace.lastExchange
        breadcrumb(
            "${trace.worker} session=${trace.sessionId} specimen=${trace.specimenId} " +
                "image=${trace.imageId} step=${trace.step} method=${exchange?.method} " +
                "endpoint=${exchange?.endpoint} status=${exchange?.statusCode} " +
                "request=${exchange?.requestBody} response=${exchange?.responseBody}",
        )
    }

    fun rememberChunkFailure(trace: UploadTrace?, error: Throwable): NetworkError {
        val connection = (error as? ProtocolException)?.causingConnection
        rememberTusFailure(trace, error, connection, fallbackMethod = "PATCH")
        return when (error) {
            is ProtocolException -> chunkNetworkError(error)
            is IOException -> NetworkError.TUS_TRANSIENT_ERROR
            else -> NetworkError.UNKNOWN_ERROR
        }
    }

    private fun chunkNetworkError(error: ProtocolException): NetworkError {
        return if (error.shouldRetry()) {
            NetworkError.TUS_TRANSIENT_ERROR
        } else {
            NetworkError.TUS_PERMANENT_ERROR
        }
    }

    fun breadcrumb(message: String) {
        val entry = UploadFailureReport.truncate(message)
        Timber.w(entry)
        VectorCamCrashlytics.log(
            message = entry,
            category = "upload",
            severity = Severity.WARNING,
        )
    }
}
