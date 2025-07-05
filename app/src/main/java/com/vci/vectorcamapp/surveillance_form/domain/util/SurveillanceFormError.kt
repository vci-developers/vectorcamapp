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

    override fun toString(context: Context): String {
        val resId = when (this) {
            SITE_NOT_FOUND -> R.string.surveillance_form_error_site_not_found
            MISSING_PROGRAM_ID -> R.string.surveillance_form_error_missing_program_id
            MISSING_SESSION -> R.string.surveillance_form_error_missing_session
            LOCATION_PERMISSION_DENIED -> R.string.surveillance_form_error_location_permission_denied
            LOCATION_GPS_TIMEOUT -> R.string.surveillance_form_error_location_gps_timeout
            UNKNOWN -> R.string.surveillance_form_error_unknown
        }
        return context.getString(resId)
    }
}
