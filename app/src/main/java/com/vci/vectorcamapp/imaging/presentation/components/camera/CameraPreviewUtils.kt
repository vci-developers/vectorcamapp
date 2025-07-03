package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi

fun handleTapToFocus(
    controller: LifecycleCameraController,
    previewView: PreviewView,
    offset: Offset
) {
    val meteringPoint = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
    val action = FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF)
        .disableAutoCancel()
        .build()
    controller.cameraControl?.startFocusAndMetering(action)
}

fun autoFocusOnObject(
    controller: LifecycleCameraController,
    previewView: PreviewView,
    box: BoundingBoxUi
) {
    val centerX = box.topLeftX + box.width / 2f
    val centerY = box.topLeftY + box.height / 2f
    val point = previewView.meteringPointFactory.createPoint(centerX, centerY)
    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
        .disableAutoCancel()
        .build()
    controller.cameraControl?.startFocusAndMetering(action)
}

fun cancelFocus(controller: LifecycleCameraController) {
    controller.cameraControl?.cancelFocusAndMetering()
}

fun calculateFocusRingOffset(
    focusPoint: Offset,
    overlaySize: IntSize,
    focusBoxSize: Dp,
    density: Density
): Offset {
    with(density) {
        val focusRingSizePx = focusBoxSize.toPx()
        val containerWidthPx = overlaySize.width.toFloat()
        val containerHeightPx = overlaySize.height.toFloat()

        val initialX = focusPoint.x - (focusRingSizePx / 2)
        val initialY = focusPoint.y - (focusRingSizePx / 2)

        val clampedX = initialX.coerceIn(0f, containerWidthPx - focusRingSizePx)
        val clampedY = initialY.coerceIn(0f, containerHeightPx - focusRingSizePx)

        return Offset(clampedX.toDp().value, clampedY.toDp().value)
    }
}