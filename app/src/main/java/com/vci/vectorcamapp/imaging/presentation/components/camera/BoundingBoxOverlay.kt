package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun BoundingBoxOverlay(
    inferenceResult: InferenceResult,
    overlaySize: IntSize,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidth = MaterialTheme.dimensions.borderThicknessThick
    val halfStrokeWidth = strokeWidth / 2
    val boxColor = MaterialTheme.colors.successConfirm

    val baseXOffsetDp = with(density) { (inferenceResult.bboxTopLeftX * overlaySize.width).toDp() }
    val baseYOffsetDp = with(density) { (inferenceResult.bboxTopLeftY * overlaySize.height).toDp() }
    val baseWidthDp = with(density) { (inferenceResult.bboxWidth * overlaySize.width).toDp() }
    val baseHeightDp = with(density) { (inferenceResult.bboxHeight * overlaySize.height).toDp() }

    val xOffsetDp = baseXOffsetDp - halfStrokeWidth
    val yOffsetDp = baseYOffsetDp - halfStrokeWidth
    val widthDp = baseWidthDp + strokeWidth
    val heightDp = baseHeightDp + strokeWidth

    val formattedConfidence = "%.2f".format(inferenceResult.bboxConfidence)

    Box(modifier = modifier.offset(x = xOffsetDp, y = yOffsetDp)) {
        Text(
            text = "specimen $formattedConfidence",
            color = MaterialTheme.colors.buttonText,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(x = 0, y = -placeable.height)
                    }
                }
                .background(boxColor)
                .padding(
                    horizontal = MaterialTheme.dimensions.paddingExtraSmall,
                    vertical = MaterialTheme.dimensions.paddingExtraExtraSmall
                )
        )

        Box(
            modifier = Modifier
                .size(width = widthDp, height = heightDp)
                .border(width = strokeWidth, color = boxColor)
        )
    }
}
