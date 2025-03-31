package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError
import com.vci.vectorcamapp.imaging.domain.BoundingBoxUi

sealed interface ImagingAction {
    data class UpdateSpecimenId(val specimenId: String) : ImagingAction
    data class UpdateBoundingBoxUi(val boundingBoxUi: BoundingBoxUi?) : ImagingAction
    data class CaptureComplete(val result: Result<Bitmap, ImagingError>) : ImagingAction
    data object RetakeImage : ImagingAction
}
