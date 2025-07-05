package com.vci.vectorcamapp.incomplete_session.presentation.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.incomplete_session.domain.util.IncompleteSessionError

fun IncompleteSessionError.toString(context: Context): String {
    val resId = when(this) {
        IncompleteSessionError.SESSION_NOT_FOUND -> R.string.incomplete_session_session_not_found
        IncompleteSessionError.SESSION_RETRIEVAL_FAILED -> R.string.incomplete_session_session_retrieval_failed
        IncompleteSessionError.UNKNOWN -> R.string.incomplete_session_unknown_error
    }
    return context.getString(resId)
}