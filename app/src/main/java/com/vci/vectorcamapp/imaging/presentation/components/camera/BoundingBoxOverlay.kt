package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import com.vci.vectorcamapp.core.domain.model.BoundingBox
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi

@Composable
fun BoundingBoxOverlay(
    boundingBoxes: List<BoundingBox>,
    overlaySize: IntSize,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        boundingBoxes.forEach { box ->
            val topLeft = Offset(
                x = box.topLeftX * overlaySize.width,
                y = box.topLeftY * overlaySize.height
            )
            val boxSize = Size(
                width = box.width * overlaySize.width,
                height = box.height * overlaySize.height
            )

            drawRect(
                color = if (box.confidence > 0.8) Color.Green else Color.Red,
                topLeft = topLeft,
                size = boxSize,
                style = Stroke(width = 4f)
            )
        }
    }
}