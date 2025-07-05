package com.vci.vectorcamapp.surveillance_form.location.data

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class LocationError : Error {
    PERMISSION_DENIED,
    GPS_TIMEOUT,
    UNKNOWN;

    override fun toString(context: Context): String {
        val resId = when(this) {
            PERMISSION_DENIED -> R.string.location_error_permission_denied
            GPS_TIMEOUT -> R.string.location_error_gps_timeout
            UNKNOWN -> R.string.location_error_unknown
        }
        return context.getString(resId)
    }
}
