package com.vci.vectorcamapp.imaging.presentation

import androidx.camera.core.ImageProxy
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi

sealed interface ImagingAction {
    data class UpdateSpecimenId(val specimenId: String) : ImagingAction
    data class UpdateBoundingBoxUi(val boundingBoxUi: BoundingBoxUi?) : ImagingAction
    data class ProcessFrame(val frame: ImageProxy) : ImagingAction
    data class CaptureComplete(val result: Result<ImageProxy, ImagingError>) : ImagingAction
    data object RetakeImage : ImagingAction
}
