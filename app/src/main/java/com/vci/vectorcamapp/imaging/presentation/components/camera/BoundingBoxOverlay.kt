package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi

@Composable
fun BoundingBoxOverlay(boundingBoxUi: BoundingBoxUi, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            color = if (boundingBoxUi.confidence > 0.8) Color.Green else Color.Red,
            topLeft = Offset(boundingBoxUi.topLeftX, boundingBoxUi.topLeftY),
            size = Size(boundingBoxUi.width, boundingBoxUi.height),
            style = Stroke(width = 4f)
        )
    }
}