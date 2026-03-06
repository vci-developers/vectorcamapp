package com.vci.vectorcamapp.imaging.data.camera

import android.view.View
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.imaging.domain.camera.CameraFocusController
import kotlin.math.max
import kotlin.math.min

class CameraFocusControllerImplementation(
    private val cameraControl: CameraControl?,
    private val cameraInfo: CameraInfo?,
    private val view: View,
    private val width: Float,
    private val height: Float
) : CameraFocusController {

    override fun focusAt(offset: Offset) {
        if (cameraControl == null || cameraInfo == null) return
        if (width <= 0 || height <= 0) return

        val clampedNormalizedX = min(1f, max(0f, offset.x))
        val clampedNormalizedY = min(1f, max(0f, offset.y))

        val display = view.display ?: return

        val factory = DisplayOrientedMeteringPointFactory(
            display,
            cameraInfo,
            width,
            height
        )

        val point = factory.createPoint(clampedNormalizedX * width, clampedNormalizedY * height)

        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .disableAutoCancel()
            .build()

        cameraControl.startFocusAndMetering(action)
    }

    override fun cancelFocus() {
        cameraControl?.cancelFocusAndMetering()
    }
}
