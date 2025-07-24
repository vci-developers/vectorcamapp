package com.vci.vectorcamapp.core.presentation.util

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize

fun Modifier.zoomPanGesture(
    containerSize: IntSize,
    minScale: Float = 1f,
    maxScale: Float = 5f
): Modifier = composed {
    val scale = remember { mutableFloatStateOf(1f) }
    val offset = remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale.floatValue * zoomChange).coerceIn(minScale, maxScale)
        scale.floatValue = newScale

        val maxX = (containerSize.width * (newScale - 1f)) / 2
        val maxY = (containerSize.height * (newScale - 1f)) / 2
        val newOffset = offset.value + panChange
        offset.value = Offset(
            newOffset.x.coerceIn(-maxX, maxX),
            newOffset.y.coerceIn(-maxY, maxY)
        )
    }

    val canPan: (Offset) -> Boolean = { delta ->
        val maxX = (containerSize.width * (scale.floatValue - 1f)) / 2
        val maxY = (containerSize.height * (scale.floatValue - 1f)) / 2
        val atLeft = offset.value.x <= -maxX + 0.5f && delta.x < 0
        val atRight = offset.value.x >= maxX - 0.5f && delta.x > 0
        val atTop = offset.value.y <= -maxY + 0.5f && delta.y < 0
        val atBottom = offset.value.y >= maxY - 0.5f && delta.y > 0
        !(atLeft || atRight || atTop || atBottom)
    }

    this
        .transformable(
            state = state,
            canPan = canPan,
            lockRotationOnZoomPan = true
        )
        .graphicsLayer {
            scaleX = scale.floatValue
            scaleY = scale.floatValue
            translationX = offset.value.x
            translationY = offset.value.y
        }
}
