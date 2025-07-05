package com.vci.vectorcamapp.main.domain.util

import android.content.Context
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error

enum class MainError : Error {
    DEVICE_FETCH_FAILED,
    UNKNOWN;
}
