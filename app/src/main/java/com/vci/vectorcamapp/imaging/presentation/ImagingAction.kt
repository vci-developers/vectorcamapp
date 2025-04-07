package com.vci.vectorcamapp.imaging.presentation

import androidx.camera.core.ImageProxy
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError

sealed interface ImagingAction {
    data class ProcessFrame(val frame: ImageProxy) : ImagingAction
    data object CaptureStart : ImagingAction
    data class CaptureComplete(val result: Result<ImageProxy, ImagingError>) : ImagingAction
    data object RetakeImage : ImagingAction
}
