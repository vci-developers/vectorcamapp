package com.vci.vectorcamapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    tertiary = accent,
    onTertiary = onAccent,
    background = background,
    onBackground = onBackground,
    surface = headerGradientTopLeft,
    surfaceVariant = headerGradientBottomRight,
    surfaceContainer = surfaceContainer,
    onSurface = onHeader,
    onSurfaceVariant = onHeaderVariant,
    primaryContainer = buttonGradientLeft,
    secondaryContainer = buttonGradientRight,
    onPrimaryContainer = onButton,
    onSecondaryContainer = onButton,
    tertiaryContainer = successConfirm,
    onTertiaryContainer = onSuccessConfirm,
    error = error,
    onError = onError,
    outline = fieldBorder,
    outlineVariant = divider
)

@Composable
fun VectorcamappTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDimensions provides Dimension()) {
        MaterialTheme(
            colorScheme = ColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}