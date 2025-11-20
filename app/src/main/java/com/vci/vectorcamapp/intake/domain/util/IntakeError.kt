package com.vci.vectorcamapp.intake.domain.util

import com.vci.vectorcamapp.core.domain.util.Error

enum class IntakeError : Error {
    SITE_NOT_FOUND,
    PROGRAM_NOT_FOUND,
    MISSING_COLLECTOR,
    COLLECTOR_SAVE_FAILED,
    LOCATION_PERMISSION_DENIED,
    LOCATION_GPS_TIMEOUT,
    FORM_INVALID,
    UNKNOWN_ERROR;
}
