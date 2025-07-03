package com.vci.vectorcamapp.core.presentation.util.error

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.SharedFlow

val LocalErrorMessageFlow = staticCompositionLocalOf<SharedFlow<ErrorMessage>> {
    error("No ErrorMessageFlow provided")
}
