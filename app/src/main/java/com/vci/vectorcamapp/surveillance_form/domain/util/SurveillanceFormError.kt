package com.vci.vectorcamapp.surveillance_form.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class SurveillanceFormError : Error {
    SITE_NOT_FOUND,
    MISSING_PROGRAM_ID,
    MISSING_SESSION,
    UNKNOWN;

    override fun toString(context: Context): String {
        val resId = when (this) {
            SITE_NOT_FOUND -> R.string.surveillance_form_error_site_not_found
            MISSING_PROGRAM_ID -> R.string.surveillance_form_error_missing_program_id
            MISSING_SESSION -> R.string.surveillance_form_error_missing_session
            UNKNOWN -> R.string.surveillance_form_error_unknown
        }
        return context.getString(resId)
    }
}
