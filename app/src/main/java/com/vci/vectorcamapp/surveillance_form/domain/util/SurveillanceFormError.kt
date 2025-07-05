package com.vci.vectorcamapp.surveillance_form.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class SurveillanceFormError : Error {
    SITE_NOT_FOUND,
    MISSING_PROGRAM_ID,
    MISSING_SESSION,
    LOCATION_PERMISSION_DENIED,
    LOCATION_GPS_TIMEOUT,
    UNKNOWN;
}
