package com.vci.vectorcamapp.core.presentation.util.success

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSuccessMessageEmitter = staticCompositionLocalOf<SuccessMessageEmitter> {
    error("No SuccessMessageEmitter provided")
}
