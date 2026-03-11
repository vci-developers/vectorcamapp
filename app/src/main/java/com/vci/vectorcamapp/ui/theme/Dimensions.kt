package com.vci.vectorcamapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimensions(
    private val scale: Float = 1f,

    // Padding & Spacing
    val paddingExtraSmall: Dp = (4 * scale).dp,
    val paddingSmall: Dp = (8 * scale).dp,
    val paddingMedium: Dp = (16 * scale).dp,
    val paddingLarge: Dp = (24 * scale).dp,
    val paddingExtraLarge: Dp = (32 * scale).dp,
    val paddingExtraExtraLarge: Dp = (48 * scale).dp,
    val spacingExtraExtraSmall: Dp = (2 * scale).dp,
    val spacingExtraSmall: Dp = (8 * scale).dp,
    val spacingSmall: Dp = (12 * scale).dp,
    val spacingMedium: Dp = (16 * scale).dp,
    val spacingLarge: Dp = (32 * scale).dp,
    val spacingExtraLarge: Dp = (48 * scale).dp,
    val spacingExtraExtraLarge: Dp = (64 * scale).dp,
    val spacingExtraExtraExtraLarge: Dp = (72 * scale).dp,

    // Component Heights
    val componentHeightExtraExtraExtraSmall: Dp = (8 * scale).dp,
    val componentHeightExtraExtraSmall: Dp = (16 * scale).dp,
    val componentHeightExtraSmall: Dp = (24 * scale).dp,
    val componentHeightSmall: Dp = (36 * scale).dp,
    val componentHeightMedium: Dp = (48 * scale).dp,
    val componentHeightLarge: Dp = (60 * scale).dp,
    val componentHeightExtraLarge: Dp = (72 * scale).dp,
    val componentHeightExtraExtraLarge: Dp = (84 * scale).dp,
    val componentHeightExtraExtraExtraLarge: Dp = (240 * scale).dp,

    // Icon Sizes
    val iconSizeExtraSmall: Dp = (14 * scale).dp,
    val iconSizeSmall: Dp = (16 * scale).dp,
    val iconSizeMedium: Dp = (18 * scale).dp,
    val iconSizeLarge: Dp = (24 * scale).dp,
    val iconSizeExtraLarge: Dp = (32 * scale).dp,
    val iconSizeExtraExtraLarge: Dp = (64 * scale).dp,

    // Corner Radii
    val cornerRadiusSmall: Dp = (8 * scale).dp,
    val cornerRadiusMedium: Dp = (16 * scale).dp,
    val cornerRadiusLarge: Dp = (24 * scale).dp,

    // Elevation / Shadows
    val shadowOffsetExtraSmall: Dp = (1 * scale).dp,
    val shadowOffsetSmall: Dp = (2 * scale).dp,
    val shadowOffsetMedium: Dp = (4 * scale).dp,
    val shadowOffsetLarge: Dp = (8 * scale).dp,
    val shadowBlurSmall: Dp = (4 * scale).dp,
    val shadowBlurMedium: Dp = (8 * scale).dp,
    val shadowBlurLarge: Dp = (16 * scale).dp,
    val shadowSpreadSmall: Dp = (2 * scale).dp,
    val shadowSpreadMedium: Dp = (4 * scale).dp,
    val shadowSpreadLarge: Dp = (6 * scale).dp,
    val elevationSmall: Dp = (2 * scale).dp,
    val elevationMedium: Dp = (4 * scale).dp,
    val elevationLarge: Dp = (8 * scale).dp,

    // Divider / Border
    val dividerThicknessThin: Dp = (1 * scale).dp,
    val dividerThicknessThick: Dp = (2 * scale).dp,
    val borderThicknessThin: Dp = (1 * scale).dp,
    val borderThicknessThick: Dp = (2 * scale).dp,

    // Typography-related spacing
    val lineHeightSmall: Dp = (16 * scale).dp,
    val lineHeightMedium: Dp = (24 * scale).dp,
    val lineHeightLarge: Dp = (32 * scale).dp,

    // Aspect Ratio Height to Width
    val aspectRatio: Float = 4f / 3f,

    // Capture Animation Parameters
    val scannerLineHeight: Dp = (3 * scale).dp,
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }

@Composable
fun screenWidthFraction(fraction: Float): Dp {
    val displayMetrics = LocalResources.current.displayMetrics
    val screenWidthDp = displayMetrics.widthPixels.toFloat() / displayMetrics.density
    return remember(fraction, screenWidthDp) { (screenWidthDp * fraction).dp }
}

@Composable
fun screenHeightFraction(fraction: Float): Dp {
    val displayMetrics = LocalResources.current.displayMetrics
    val screenHeightDp = displayMetrics.heightPixels.toFloat() / displayMetrics.density
    return remember(fraction, screenHeightDp) { (screenHeightDp * fraction).dp }
}
