package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError

sealed interface ImagingAction {
    data class CaptureComplete(val result: Result<Bitmap, ImagingError>): ImagingAction
}
