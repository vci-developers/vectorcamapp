package com.vci.vectorcamapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

data class Dimensions(
    val scale: Float = 1.0f,
) {
    // Padding & Spacing
    val paddingExtraSmall: Dp get() = (4 * scale).dp
    val paddingSmall: Dp get() = (8 * scale).dp
    val paddingMedium: Dp get() = (16 * scale).dp
    val paddingLarge: Dp get() = (24 * scale).dp
    val paddingExtraLarge: Dp get() = (32 * scale).dp
    val paddingExtraExtraLarge: Dp get() = (48 * scale).dp
    val spacingExtraExtraSmall: Dp get() = (2 * scale).dp
    val spacingExtraSmall: Dp get() = (8 * scale).dp
    val spacingSmall: Dp get() = (12 * scale).dp
    val spacingMedium: Dp get() = (16 * scale).dp
    val spacingLarge: Dp get() = (32 * scale).dp
    val spacingExtraLarge: Dp get() = (48 * scale).dp
    val spacingExtraExtraLarge: Dp get() = (64 * scale).dp
    val spacingExtraExtraExtraLarge: Dp get() = (72 * scale).dp

    // Component Heights
    val componentHeightExtraExtraExtraSmall: Dp get() = (8 * scale).dp
    val componentHeightExtraExtraSmall: Dp get() = (16 * scale).dp
    val componentHeightExtraSmall: Dp get() = (24 * scale).dp
    val componentHeightSmall: Dp get() = (36 * scale).dp
    val componentHeightMedium: Dp get() = (48 * scale).dp
    val componentHeightLarge: Dp get() = (60 * scale).dp
    val componentHeightExtraLarge: Dp get() = (72 * scale).dp
    val componentHeightExtraExtraLarge: Dp get() = (84 * scale).dp
    val componentHeightExtraExtraExtraLarge: Dp get() = (120 * scale).dp

    // Icon Sizes
    private val iconScale: Float get() = lerp(1f, scale, 0.5f)
    val iconSizeExtraSmall: Dp get() = (14 * iconScale).dp
    val iconSizeSmall: Dp get() = (16 * iconScale).dp
    val iconSizeMedium: Dp get() = (18 * iconScale).dp
    val iconSizeLarge: Dp get() = (24 * iconScale).dp
    val iconSizeExtraLarge: Dp get() = (32 * iconScale).dp
    val iconSizeExtraExtraLarge: Dp get() = (64 * iconScale).dp

    // Corner Radii
    val cornerRadiusSmall: Dp get() = 8.dp
    val cornerRadiusMedium: Dp get() = 16.dp
    val cornerRadiusLarge: Dp get() = 24.dp

    // Elevation / Shadows
    val shadowOffsetExtraSmall: Dp = 1.dp
    val shadowOffsetSmall: Dp = 2.dp
    val shadowOffsetMedium: Dp = 4.dp
    val shadowOffsetLarge: Dp = 8.dp
    val shadowBlurSmall: Dp = 4.dp
    val shadowBlurMedium: Dp = 8.dp
    val shadowBlurLarge: Dp = 16.dp
    val shadowSpreadSmall: Dp = 2.dp
    val shadowSpreadMedium: Dp = 4.dp
    val shadowSpreadLarge: Dp = 6.dp
    val elevationSmall: Dp = 2.dp
    val elevationMedium: Dp = 4.dp
    val elevationLarge: Dp = 8.dp

    // Divider / Border
    val dividerThickness: Dp = 2.dp
    val borderThicknessThin: Dp = 1.dp
    val borderThicknessThick: Dp = 2.dp

    // Typography-related spacing
    val lineHeightSmall: Dp get() = (16 * scale).dp
    val lineHeightMedium: Dp get() = (24 * scale).dp
    val lineHeightLarge: Dp get() = (32 * scale).dp

    // Aspect Ratio Height to Width
    val aspectRatio: Float = 4f / 3f

    // Capture Animation Parameters
    val scannerLineHeight: Dp = 3.dp
}

val LocalDimensions = staticCompositionLocalOf { Dimensions() }

@Composable
fun ProvideDimensions(content: @Composable () -> Unit) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val dimensions = remember(windowInfo.containerSize) {
        val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp().value }
        val scale = when {
            screenWidthDp < 360 -> 0.85f
            screenWidthDp < 600 -> 1.0f
            screenWidthDp < 840 -> 1.15f
            else -> 1.3f
        }
        Dimensions(scale = scale)
    }

    CompositionLocalProvider(LocalDimensions provides dimensions) {
        content()
    }
}

@Composable
fun screenWidthFraction(fraction: Float): Dp {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val screenWidthDp = displayMetrics.widthPixels.toFloat() / displayMetrics.density
    return remember(fraction, screenWidthDp) { (screenWidthDp * fraction).dp }
}

@Composable
fun screenHeightFraction(fraction: Float): Dp {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val screenHeightDp = displayMetrics.heightPixels.toFloat() / displayMetrics.density
    return remember(fraction, screenHeightDp) { (screenHeightDp * fraction).dp }
}
