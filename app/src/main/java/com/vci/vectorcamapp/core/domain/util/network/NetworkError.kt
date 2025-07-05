package com.vci.vectorcamapp.core.domain.util.network

import androidx.annotation.StringRes
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class NetworkError(@StringRes override val messageResId: Int): Error {
    // Generic Errors
    REQUEST_TIMEOUT(R.string.network_error_request_timeout),
    NOT_FOUND(R.string.network_error_not_found),
    TOO_MANY_REQUESTS(R.string.network_error_too_many_requests),
    NO_INTERNET(R.string.network_error_no_internet),
    CONFLICT(R.string.network_error_conflict),
    CLIENT_ERROR(R.string.network_error_client_error),
    SERVER_ERROR(R.string.network_error_server_error),
    SERIALIZATION(R.string.network_error_serialization_error),
    UNKNOWN_ERROR(R.string.main_error_unknown_error),

    // Endpoint-Specific Errors
    SESSION_NOT_COMPLETED(R.string.network_error_session_not_completed);
}
