package com.vci.vectorcamapp.core.presentation.util.imaging

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError

fun ImagingError.toString(context: Context): String {
    val resId = when(this) {
        ImagingError.CANNOT_CAPTURE -> R.string.imaging_error_cannot_capture
    }
    return context.getString(resId)
}
