package com.vci.vectorcamapp.core.domain.util.network

import com.vci.vectorcamapp.core.domain.util.Error

enum class NetworkError : Error {
    REQUEST_TIMEOUT, TOO_MANY_REQUESTS, NO_INTERNET, SERVER_ERROR, SERIALIZATION, UNKNOWN,
}
