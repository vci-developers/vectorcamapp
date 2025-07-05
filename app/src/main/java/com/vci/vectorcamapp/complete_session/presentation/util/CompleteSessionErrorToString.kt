package com.vci.vectorcamapp.complete_session.presentation.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.complete_session.domain.util.CompleteSessionError

fun CompleteSessionError.toString(context: Context): String {
    val resId = when(this) {
        CompleteSessionError.SESSION_NOT_FOUND -> R.string.complete_session_session_not_found
        CompleteSessionError.SITE_NOT_FOUND -> R.string.complete_session_site_not_found
        CompleteSessionError.SURVEILLANCE_FORM_NOT_FOUND -> R.string.complete_session_surveillance_form_not_found
        CompleteSessionError.UNKNOWN -> R.string.complete_session_unknown_error
    }
    return context.getString(resId)
}