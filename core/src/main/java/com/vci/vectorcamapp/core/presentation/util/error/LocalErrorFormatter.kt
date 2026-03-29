package com.vci.vectorcamapp.core.presentation.util.error

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.vci.vectorcamapp.core.domain.util.Error

val LocalErrorFormatter = staticCompositionLocalOf<(Error, Context) -> String> {
    { error, _ -> error.javaClass.simpleName }
}
