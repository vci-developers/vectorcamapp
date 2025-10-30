package com.vci.vectorcamapp.core.domain.util.collector

import com.vci.vectorcamapp.core.domain.util.Error

enum class CollectorValidationError : Error {
    BLANK_COLLECTOR_TITLE,
    BLANK_COLLECTOR_NAME,
}
