package com.vci.vectorcamapp.main.domain.util

import androidx.annotation.StringRes
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class MainError(@StringRes override val messageResId: Int): Error {
    DEVICE_FETCH_FAILED(R.string.main_error_device_fetch_failed),
    UNKNOWN_ERROR(R.string.main_error_unknown_error);
}
