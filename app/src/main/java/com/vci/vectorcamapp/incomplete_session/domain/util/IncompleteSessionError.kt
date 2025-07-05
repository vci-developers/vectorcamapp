package com.vci.vectorcamapp.incomplete_session.domain.util

import androidx.annotation.StringRes
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class IncompleteSessionError(@StringRes override val messageResId: Int): Error {
    SESSION_NOT_FOUND(R.string.incomplete_session_error_session_not_found),
    SESSION_RETRIEVAL_FAILED(R.string.incomplete_session_error_session_retrieval_failed),
    UNKNOWN_ERROR(R.string.incomplete_session_error_unknown_error);
}
