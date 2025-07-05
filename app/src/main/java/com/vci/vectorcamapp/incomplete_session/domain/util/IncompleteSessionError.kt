package com.vci.vectorcamapp.incomplete_session.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class IncompleteSessionError : Error {
    SESSION_NOT_FOUND,
    SESSION_RETRIEVAL_FAILED,
    NO_INCOMPLETE_SESSIONS,
    UNKNOWN;

    override fun toString(context: Context): String {
        val resId = when(this) {
            SESSION_NOT_FOUND -> R.string.incomplete_session_session_not_found
            SESSION_RETRIEVAL_FAILED -> R.string.incomplete_session_session_retrieval_failed
            NO_INCOMPLETE_SESSIONS -> R.string.incomplete_session_no_incomplete_sessions
            UNKNOWN -> R.string.incomplete_session_unknown_error
        }
        return context.getString(resId)
    }
}
