package com.vci.vectorcamapp.registration.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class RegistrationError : Error {
    PROGRAM_NOT_FOUND,
    UNKNOWN;

    override fun toString(context: Context): String {
        val resId = when(this) {
            PROGRAM_NOT_FOUND -> R.string.registration_program_not_found
            UNKNOWN -> R.string.registration_unknown_error
        }
        return context.getString(resId)
    }
}
