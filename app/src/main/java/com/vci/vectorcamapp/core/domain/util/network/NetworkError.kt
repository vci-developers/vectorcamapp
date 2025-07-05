package com.vci.vectorcamapp.core.domain.util.network

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class NetworkError : Error {
    // Generic Errors
    REQUEST_TIMEOUT,
    NOT_FOUND,
    TOO_MANY_REQUESTS,
    NO_INTERNET,
    CONFLICT,
    CLIENT_ERROR,
    SERVER_ERROR,
    SERIALIZATION,
    UNKNOWN,

    // Endpoint-Specific Errors
    SESSION_NOT_COMPLETED;

    override fun toString(context: Context): String {
        val resId = when(this) {
            REQUEST_TIMEOUT -> R.string.network_error_request_timeout
            TOO_MANY_REQUESTS -> R.string.network_error_too_many_requests
            NO_INTERNET -> R.string.network_error_no_internet
            SERVER_ERROR -> R.string.network_error_server_error
            SERIALIZATION -> R.string.network_error_serialization
            SESSION_NOT_COMPLETED -> R.string.network_error_session_not_completed
            NOT_FOUND -> R.string.network_error_not_found
            CONFLICT -> R.string.network_error_conflict
            CLIENT_ERROR -> R.string.network_error_client_error
            UNKNOWN -> R.string.network_error_unknown
        }
        return context.getString(resId)
    }
}
