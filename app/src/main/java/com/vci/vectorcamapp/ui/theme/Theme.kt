package com.vci.vectorcamapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.vci.vectorcamapp.core.presentation.LocalCrashyContext

@Composable
fun VectorcamappTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColors provides Colors(),
        LocalDimensions provides Dimensions(),
        LocalCrashyContext provides null
    ) {
        MaterialTheme(
            typography = AppTypography,
            content = content
        )
    }
}
