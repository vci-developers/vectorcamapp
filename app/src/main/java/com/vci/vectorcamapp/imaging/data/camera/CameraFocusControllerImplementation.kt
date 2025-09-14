package com.vci.vectorcamapp.imaging.data.camera

import android.util.Log
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.StreamState
import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.imaging.domain.camera.CameraFocusController
import kotlin.math.max
import kotlin.math.min

class CameraFocusControllerImplementation (
    private val previewView: PreviewView,
    private val controller: LifecycleCameraController,
) : CameraFocusController {

    override fun focusAt(offset: Offset) {
        if (previewView.previewStreamState.value != StreamState.STREAMING) {
            Log.d("CameraFocusController", "focusAt: stream not ready; skip")
            return
        }
        val previewWidth = previewView.width
        val previewHeight = previewView.height
        if (previewWidth == 0 || previewHeight == 0) {
            Log.d("CameraFocusController", "focusAt: preview size 0; skip")
            return
        }

        val clampedNormalizedX = min(1f, max(0f, offset.x))
        val clampedNormalizedY = min(1f, max(0f, offset.y))

        val focusPixelX = clampedNormalizedX * previewWidth
        val focusPixelY = clampedNormalizedY * previewHeight

        Log.d(
            "CameraFocusController",
            "focusAt: normalized=($clampedNormalizedX,$clampedNormalizedY) -> px=($focusPixelX,$focusPixelY)"
        )

        val action = buildFocusAction(focusPixelX, focusPixelY)
        controller.cameraControl
            ?.startFocusAndMetering(action)
            ?: Log.w("CameraFocusController", "focusAt: cameraControl not ready yet")
    }

    private fun buildFocusAction(x: Float, y: Float): FocusMeteringAction {
        val point = previewView.meteringPointFactory.createPoint(x, y)
        return FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .disableAutoCancel()
            .build()
    }

    override fun cancelFocus() {
        controller.cameraControl
            ?.cancelFocusAndMetering()
            ?: Log.w("CameraFocusManager", "cancelFocus(): cameraControl not ready yet")
    }
}
