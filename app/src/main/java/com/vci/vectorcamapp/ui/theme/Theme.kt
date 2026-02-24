package com.vci.vectorcamapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun VectorcamappTheme(
    windowType: WindowType = WindowType.Compact,
    content: @Composable () -> Unit
) {
    val scale = when (windowType) {
        WindowType.Compact -> 1.0f
        WindowType.Medium -> 1.15f
        WindowType.Expanded -> 1.2f
    }

    val dimensions = Dimensions(scale = scale)
    val typography = createTypography(scale = scale)

    CompositionLocalProvider(
        LocalColors provides Colors(),
        LocalDimensions provides dimensions
    ) {
        MaterialTheme(
            typography = typography,
            content = content
        )
    }
}
