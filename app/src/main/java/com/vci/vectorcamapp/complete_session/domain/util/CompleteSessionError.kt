package com.vci.vectorcamapp.complete_session.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class CompleteSessionError : Error {
    SESSION_NOT_FOUND,
    SITE_NOT_FOUND,
    SURVEILLANCE_FORM_NOT_FOUND,
    UNKNOWN;
}
