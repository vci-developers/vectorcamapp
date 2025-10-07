package com.vci.vectorcamapp.settings.domain.util

import com.vci.vectorcamapp.core.domain.util.Error

enum class SettingsValidationError : Error {
    BLANK_COLLECTOR_TITLE,
    BLANK_COLLECTOR_NAME,
}
