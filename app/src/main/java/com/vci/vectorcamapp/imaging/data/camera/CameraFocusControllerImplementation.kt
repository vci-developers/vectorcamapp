package com.vci.vectorcamapp.imaging.data.camera

import android.util.Log
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.PreviewView.StreamState
import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.imaging.domain.camera.CameraFocusController

class CameraFocusControllerImplementation (
    private val previewView: PreviewView,
    private val controller: LifecycleCameraController,
) : CameraFocusController {

    override fun manualFocusAt(offset: Offset) {
        controller.cameraControl
            ?.startFocusAndMetering(buildFocusAction(offset.x, offset.y))
            ?: Log.w("CameraFocusManager", "focusAt(): cameraControl not ready yet")
    }

    override fun autoFocusAt(offset: Offset) {
        if (previewView.previewStreamState.value != StreamState.STREAMING) {
            Log.d("CameraFocusManager", "autoFocusAt(normalized): stream not ready; skip")
            return
        }
        val w = previewView.width
        val h = previewView.height
        if (w == 0 || h == 0) {
            Log.d("CameraFocusManager", "autoFocusAt(normalized): preview size 0; skip")
            return
        }

        val fx = offset.x * w
        val fy = offset.y * h
        Log.d("CameraFocusManager", "autoFocusAt(normalized): focusing at px=($fx,$fy) from norm=(${offset.x},${offset.y})")

        val action = buildFocusAction(fx, fy)
        controller.cameraControl
            ?.startFocusAndMetering(action)
            ?: Log.w("CameraFocusManager", "autoFocusAt(normalized): cameraControl not ready yet")
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
