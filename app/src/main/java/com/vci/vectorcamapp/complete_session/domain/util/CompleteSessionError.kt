package com.vci.vectorcamapp.complete_session.domain.util

import androidx.annotation.StringRes
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class CompleteSessionError(@StringRes override val messageResId: Int) : Error {
    SESSION_NOT_FOUND(R.string.complete_session_error_session_not_found),
    SITE_NOT_FOUND(R.string.complete_session_error_site_not_found),
    SURVEILLANCE_FORM_NOT_FOUND(R.string.complete_session_error_surveillance_form_not_found),
    UNKNOWN_ERROR(R.string.complete_session_error_unknown_error);
}
