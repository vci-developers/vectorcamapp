package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun TrayAlignmentGuideOverlay(
    overlaySize: IntSize,
    modifier: Modifier = Modifier
) {
    val specimenIdTopFraction = 0.06f
    val specimenIdHeightFraction = 0.16f
    val specimenIdWidthFraction = 0.75f
    val trayGapTopFraction = 0.06f
    val trayHeightFraction = 0.65f
    val trayWidthFraction = 0.85f

    val density = LocalDensity.current
    val strokeWidth = with(density) { MaterialTheme.dimensions.borderThicknessThick.toPx() }
    val cornerRadius = with(density) { MaterialTheme.dimensions.cornerRadiusMedium.toPx() }
    val dashInterval = with(density) { MaterialTheme.dimensions.spacingMedium.toPx() }
    val dashGap = with(density) { MaterialTheme.dimensions.spacingExtraSmall.toPx() }
    val guideColor = MaterialTheme.colors.accent.copy(alpha = 0.5f)

    val width = overlaySize.width.toFloat()
    val height = overlaySize.height.toFloat()

    val specimenIdTopMargin = height * specimenIdTopFraction
    val specimenIdHeight = height * specimenIdHeightFraction
    val specimenIdWidth = width * specimenIdWidthFraction
    val specimenIdLeft = (width - specimenIdWidth) / 2f

    val trayTopMargin = specimenIdTopMargin + specimenIdHeight + (height * trayGapTopFraction)
    val trayHeight = height * trayHeightFraction
    val trayWidth = width * trayWidthFraction
    val trayLeft = (width - trayWidth) / 2f

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(dashInterval, dashGap), 0f)

            drawRoundRect(
                color = guideColor,
                topLeft = Offset(specimenIdLeft, specimenIdTopMargin),
                size = Size(specimenIdWidth, specimenIdHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = strokeWidth, pathEffect = dashEffect)
            )

            drawRoundRect(
                color = guideColor,
                topLeft = Offset(trayLeft, trayTopMargin),
                size = Size(trayWidth, trayHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = strokeWidth, pathEffect = dashEffect)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(with(density) { specimenIdWidth.toDp() })
                .height(with(density) { specimenIdHeight.toDp() })
                .offset(
                    x = with(density) { specimenIdLeft.toDp() },
                    y = with(density) { specimenIdTopMargin.toDp() }
                )
        ) {
            Text(
                text = "ABC123",
                style = MaterialTheme.typography.displayLarge,
                color = guideColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(with(density) { trayWidth.toDp() })
                .height(with(density) { trayHeight.toDp() })
                .offset(
                    x = with(density) { trayLeft.toDp() },
                    y = with(density) { trayTopMargin.toDp() }
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_specimen),
                contentDescription = "Mosquito alignment guide",
                tint = guideColor,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeExtraExtraExtraLarge)
            )
        }
    }
}
