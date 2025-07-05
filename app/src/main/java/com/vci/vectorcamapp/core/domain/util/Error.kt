package com.vci.vectorcamapp.core.domain.util

import android.content.Context

interface Error {
    fun toString(context: Context): String
}