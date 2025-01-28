package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError
import com.vci.vectorcamapp.imaging.domain.Detection

sealed interface ImagingAction {
    data class UpdateDetection(val detection: Detection?) : ImagingAction
    data class CaptureComplete(val result: Result<Bitmap, ImagingError>): ImagingAction
}
