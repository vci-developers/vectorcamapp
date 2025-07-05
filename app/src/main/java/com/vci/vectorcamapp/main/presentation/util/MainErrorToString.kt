package com.vci.vectorcamapp.main.presentation.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.main.domain.util.MainError

fun MainError.toString(context: Context): String {
    val resId = when(this) {
        MainError.DEVICE_FETCH_FAILED -> R.string.main_error_device_fetch_failed
        MainError.UNKNOWN -> R.string.main_error_unknown
    }
    return context.getString(resId)
}