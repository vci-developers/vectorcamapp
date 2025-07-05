package com.vci.vectorcamapp.registration.presentation.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.registration.domain.util.RegistrationError

fun RegistrationError.toString(context: Context): String {
    val resId = when(this) {
        RegistrationError.PROGRAM_NOT_FOUND -> R.string.registration_program_not_found
        RegistrationError.UNKNOWN -> R.string.registration_unknown_error
    }
    return context.getString(resId)
}