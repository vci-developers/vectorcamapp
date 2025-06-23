package com.vci.vectorcamapp.surveillance_form.location.data

import android.content.Context
import com.vci.vectorcamapp.R

fun LocationError.toString(context: Context): String {
    val resId = when(this) {
        LocationError.PERMISSION_DENIED -> R.string.location_permission_error
        LocationError.GPS_TIMEOUT -> R.string.location_gps_timeout
        LocationError.UNKNOWN -> R.string.location_unknown
    }
    return context.getString(resId)
}
