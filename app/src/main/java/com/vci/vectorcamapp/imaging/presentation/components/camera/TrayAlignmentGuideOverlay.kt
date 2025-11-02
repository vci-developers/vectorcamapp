package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    hasSpecimenId: Boolean,
    hasSpecimenImage: Boolean,
    modifier: Modifier = Modifier
) {
    val specimenIdTopFraction = 0.06f
    val specimenIdHeightFraction = 0.16f
    val specimenIdWidthFraction = 0.75f
    val trayGapTopFraction = 0.15f
    val trayWidthFraction = 0.55f

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
    val traySize = width * trayWidthFraction
    val trayLeft = (width - traySize) / 2f

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(dashInterval, dashGap), 0f)

            if (!hasSpecimenId) {
                drawRoundRect(
                    color = guideColor,
                    topLeft = Offset(specimenIdLeft, specimenIdTopMargin),
                    size = Size(specimenIdWidth, specimenIdHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = strokeWidth, pathEffect = dashEffect)
                )
            }

            if (!hasSpecimenImage) {
                drawRoundRect(
                    color = guideColor,
                    topLeft = Offset(trayLeft, trayTopMargin),
                    size = Size(traySize, traySize),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = strokeWidth, pathEffect = dashEffect)
                )
            }
        }

        if (!hasSpecimenId) {
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
        }

        if (!hasSpecimenImage) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(with(density) { traySize.toDp() })
                    .height(with(density) { traySize.toDp() })
                    .offset(
                        x = with(density) { trayLeft.toDp() },
                        y = with(density) { trayTopMargin.toDp() }
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_specimen),
                    contentDescription = "Mosquito alignment guide",
                    tint = guideColor,
                    modifier = Modifier.size(MaterialTheme.dimensions.iconSizeExtraExtraLarge)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(MaterialTheme.dimensions.paddingMedium),
            shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
            color = MaterialTheme.colors.appBackground.copy(alpha = 0.95f),
            shadowElevation = MaterialTheme.dimensions.elevationSmall
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingSmall),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.paddingExtraSmall)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasSpecimenId,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colors.successConfirm,
                            uncheckedColor = MaterialTheme.colors.disabled
                        )
                    )
                    Text(
                        text = "Specimen ID",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasSpecimenId) MaterialTheme.colors.textPrimary else MaterialTheme.colors.textSecondary,
                        modifier = Modifier.padding(start = MaterialTheme.dimensions.paddingSmall)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasSpecimenImage,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colors.successConfirm,
                            uncheckedColor = MaterialTheme.colors.disabled
                        )
                    )
                    Text(
                        text = "Specimen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasSpecimenImage) MaterialTheme.colors.textPrimary else MaterialTheme.colors.textSecondary,
                        modifier = Modifier.padding(start = MaterialTheme.dimensions.paddingSmall)
                    )
                }
            }
        }
    }
}
