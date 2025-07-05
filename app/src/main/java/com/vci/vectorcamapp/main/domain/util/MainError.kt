package com.vci.vectorcamapp.main.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class MainError : Error {
    DEVICE_FETCH_FAILED,
    UNKNOWN;

    override fun toString(context: Context): String {
        val resId = when(this) {
            DEVICE_FETCH_FAILED -> R.string.main_error_device_fetch_failed
            UNKNOWN -> R.string.main_error_unknown
        }
        return context.getString(resId)
    }
}
