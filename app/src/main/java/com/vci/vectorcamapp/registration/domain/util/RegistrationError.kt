package com.vci.vectorcamapp.registration.domain.util

import androidx.annotation.StringRes
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class RegistrationError(@StringRes override val messageResId: Int): Error {
    PROGRAM_NOT_FOUND(R.string.registration_error_program_not_found),
    UNKNOWN_ERROR(R.string.registration_error_unknown_error);
}
