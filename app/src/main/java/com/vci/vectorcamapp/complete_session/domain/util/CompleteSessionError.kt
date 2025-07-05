package com.vci.vectorcamapp.complete_session.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class CompleteSessionError : Error {
    SESSION_NOT_FOUND,
    SITE_NOT_FOUND,
    SURVEILLANCE_FORM_NOT_FOUND,
    UNKNOWN;

    override fun toString(context: Context): String {
        val resId = when(this) {
            SESSION_NOT_FOUND -> R.string.complete_session_session_not_found
            SITE_NOT_FOUND -> R.string.complete_session_site_not_found
            SURVEILLANCE_FORM_NOT_FOUND -> R.string.complete_session_surveillance_form_not_found
            UNKNOWN -> R.string.complete_session_unknown_error
        }
        return context.getString(resId)
    }
}
