package com.vci.vectorcamapp.imaging.data.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import com.vci.vectorcamapp.imaging.domain.camera.CameraMetadataListener
import com.vci.vectorcamapp.imaging.domain.model.AfRegion
import com.vci.vectorcamapp.imaging.domain.model.CameraMetadata
import com.vci.vectorcamapp.imaging.domain.model.ColorCorrectionGains
import java.util.concurrent.atomic.AtomicReference

class CameraMetadataListenerImplementation : CameraCaptureSession.CaptureCallback(),
    CameraMetadataListener {

    private val _latestMetadata = AtomicReference<CameraMetadata?>(null)

    override val latestMetadata: CameraMetadata?
        get() = _latestMetadata.get()

    override fun reset() {
        _latestMetadata.set(null)
    }

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        val rggb = result[CaptureResult.COLOR_CORRECTION_GAINS]
        _latestMetadata.set(
            CameraMetadata(
                focusDistance = result[CaptureResult.LENS_FOCUS_DISTANCE],
                aperture = result[CaptureResult.LENS_APERTURE],
                exposureTimeNs = result[CaptureResult.SENSOR_EXPOSURE_TIME],
                iso = result[CaptureResult.SENSOR_SENSITIVITY],
                colorCorrectionGains = rggb?.let {
                    ColorCorrectionGains(
                        red = it.red,
                        greenEven = it.greenEven,
                        greenOdd = it.greenOdd,
                        blue = it.blue
                    )
                },
                awbMode = result[CaptureResult.CONTROL_AWB_MODE],
                afRegions = result[CaptureResult.CONTROL_AF_REGIONS]?.map { region ->
                    AfRegion(
                        x = region.x,
                        y = region.y,
                        width = region.width,
                        height = region.height,
                        weight = region.meteringWeight
                    )
                } ?: emptyList()
            )
        )
    }
}
