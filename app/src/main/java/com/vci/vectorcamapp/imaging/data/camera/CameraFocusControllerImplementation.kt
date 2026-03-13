package com.vci.vectorcamapp.imaging.data.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.view.View
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.imaging.domain.camera.CameraFocusController
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalCamera2Interop::class)
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

    override fun setFocusDistance(normalizedDistance: Float?) {
        val camera2Control = cameraControl?.let { Camera2CameraControl.from(it) } ?: return

        if (normalizedDistance == null) {
            // Clear manual overrides and restore continuous autofocus
            camera2Control.setCaptureRequestOptions(CaptureRequestOptions.Builder().build())
            cameraControl?.cancelFocusAndMetering()
        } else {
            val camera2Info = cameraInfo?.let { Camera2CameraInfo.from(it) }
            // LENS_INFO_MINIMUM_FOCUS_DISTANCE is the max diopter value (closest focus)
            val maxDiopters = camera2Info
                ?.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                ?: 10f  // ~10cm default if unavailable

            // normalizedDistance: 0.0 = infinity (far), 1.0 = closest (near)
            val focusDistanceDiopters = normalizedDistance * maxDiopters

            camera2Control.setCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF
                    )
                    .setCaptureRequestOption(
                        CaptureRequest.LENS_FOCUS_DISTANCE,
                        focusDistanceDiopters
                    )
                    .build()
            )
        }
    }
}
