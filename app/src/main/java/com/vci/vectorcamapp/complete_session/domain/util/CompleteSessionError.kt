package com.vci.vectorcamapp.complete_session.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class CompleteSessionError : Error {
    SESSION_NOT_FOUND,
    SITE_NOT_FOUND,
    NO_COMPLETE_SESSIONS,
    NO_SURVEILLANCE_FORM,
    NO_SPECIMENS,
    UNKNOWN;

    override fun toString(context: Context): String {
        val resId = when(this) {
            SESSION_NOT_FOUND -> R.string.complete_session_session_not_found
            SITE_NOT_FOUND -> R.string.complete_session_site_not_found
            NO_COMPLETE_SESSIONS -> R.string.complete_session_no_complete_sessions
            NO_SURVEILLANCE_FORM -> R.string.complete_session_no_surveillance_form
            NO_SPECIMENS -> R.string.complete_session_no_specimens
            UNKNOWN -> R.string.complete_session_unknown_error
        }
        return context.getString(resId)
    }
}
