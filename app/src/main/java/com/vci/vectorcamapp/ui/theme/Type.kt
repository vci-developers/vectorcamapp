package com.vci.vectorcamapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.vci.vectorcamapp.R

val displayLargeSize = 28.sp
val displayMediumSize = 24.sp
val displaySmallSize = 22.sp
val headlineLargeSize = 20.sp
val headlineMediumSize = 18.sp
val headlineSmallSize = 17.sp
val titleLargeSize = 16.sp
val titleMediumSize = 15.sp
val titleSmallSize = 14.sp
val bodyLargeSize = 16.sp
val bodyMediumSize = 14.sp
val bodySmallSize = 12.sp
val labelLargeSize = 12.sp
val labelMediumSize = 11.sp
val labelSmallSize = 9.sp

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val bodyFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Inter"),
        fontProvider = provider,
    )
)

val displayFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Inter"),
        fontProvider = provider,
    )
)

// Default Material 3 typography values
val baseline = Typography()

fun createTypography(scale: Float = 1f): Typography {
    return Typography(
        displayLarge = baseline.displayLarge.copy(
            fontFamily = displayFontFamily,
            fontSize = displayLargeSize * scale,
            lineHeight = displayLargeSize * scale,
            fontWeight = FontWeight.Black
        ),
        displayMedium = baseline.displayMedium.copy(
            fontFamily = displayFontFamily,
            fontSize = displayMediumSize * scale,
            lineHeight = displayMediumSize * scale,
            fontWeight = FontWeight.ExtraBold
        ),
        displaySmall = baseline.displaySmall.copy(
            fontFamily = displayFontFamily,
            fontSize = displaySmallSize * scale,
            lineHeight = displaySmallSize * scale,
            fontWeight = FontWeight.Bold
        ),
        headlineLarge = baseline.headlineLarge.copy(
            fontFamily = displayFontFamily,
            fontSize = headlineLargeSize * scale,
            lineHeight = headlineLargeSize * scale,
            fontWeight = FontWeight.SemiBold
        ),
        headlineMedium = baseline.headlineMedium.copy(
            fontFamily = displayFontFamily,
            fontSize = headlineMediumSize * scale,
            lineHeight = headlineMediumSize * scale,
            fontWeight = FontWeight.SemiBold
        ),
        headlineSmall = baseline.headlineSmall.copy(
            fontFamily = displayFontFamily,
            fontSize = headlineSmallSize * scale,
            lineHeight = headlineSmallSize * scale,
            fontWeight = FontWeight.Medium
        ),
        titleLarge = baseline.titleLarge.copy(
            fontFamily = displayFontFamily,
            fontSize = titleLargeSize * scale,
            lineHeight = titleLargeSize * scale,
            fontWeight = FontWeight.Medium
        ),
        titleMedium = baseline.titleMedium.copy(
            fontFamily = displayFontFamily,
            fontSize = titleMediumSize * scale,
            lineHeight = titleMediumSize * scale,
            fontWeight = FontWeight.Medium
        ),
        titleSmall = baseline.titleSmall.copy(
            fontFamily = displayFontFamily,
            fontSize = titleSmallSize * scale,
            lineHeight = titleSmallSize * scale,
            fontWeight = FontWeight.Medium
        ),
        bodyLarge = baseline.bodyLarge.copy(
            fontFamily = bodyFontFamily,
            fontSize = bodyLargeSize * scale,
            lineHeight = bodyLargeSize * scale,
            fontWeight = FontWeight.Normal
        ),
        bodyMedium = baseline.bodyMedium.copy(
            fontFamily = bodyFontFamily,
            fontSize = bodyMediumSize * scale,
            lineHeight = bodyMediumSize * scale,
            fontWeight = FontWeight.Normal
        ),
        bodySmall = baseline.bodySmall.copy(
            fontFamily = bodyFontFamily,
            fontSize = bodySmallSize * scale,
            lineHeight = bodySmallSize * scale,
            fontWeight = FontWeight.Normal
        ),
        labelLarge = baseline.labelLarge.copy(
            fontFamily = bodyFontFamily,
            fontSize = labelLargeSize * scale,
            lineHeight = labelLargeSize * scale,
            fontWeight = FontWeight.Light
        ),
        labelMedium = baseline.labelMedium.copy(
            fontFamily = bodyFontFamily,
            fontSize = labelMediumSize * scale,
            lineHeight = labelMediumSize * scale,
            fontWeight = FontWeight.Light
        ),
        labelSmall = baseline.labelSmall.copy(
            fontFamily = bodyFontFamily,
            fontSize = labelSmallSize * scale,
            lineHeight = labelSmallSize * scale,
            fontWeight = FontWeight.Light
        ),
    )
}
