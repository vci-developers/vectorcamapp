package com.vci.vectorcamapp.surveillance_form.presentation.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.surveillance_form.domain.util.SurveillanceFormError

fun SurveillanceFormError.toString(context: Context): String {
    val resId = when (this) {
        SurveillanceFormError.SITE_NOT_FOUND -> R.string.surveillance_form_error_site_not_found
        SurveillanceFormError.MISSING_PROGRAM_ID -> R.string.surveillance_form_error_missing_program_id
        SurveillanceFormError.MISSING_SESSION -> R.string.surveillance_form_error_missing_session
        SurveillanceFormError.LOCATION_PERMISSION_DENIED -> R.string.surveillance_form_error_location_permission_denied
        SurveillanceFormError.LOCATION_GPS_TIMEOUT -> R.string.surveillance_form_error_location_gps_timeout
        SurveillanceFormError.UNKNOWN -> R.string.surveillance_form_error_unknown
    }
    return context.getString(resId)
}