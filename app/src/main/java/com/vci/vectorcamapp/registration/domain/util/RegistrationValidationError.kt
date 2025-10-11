package com.vci.vectorcamapp.registration.domain.util

import com.vci.vectorcamapp.core.domain.util.Error

enum class RegistrationValidationError : Error {
    BLANK_COLLECTOR_TITLE,
    BLANK_COLLECTOR_NAME,
}
