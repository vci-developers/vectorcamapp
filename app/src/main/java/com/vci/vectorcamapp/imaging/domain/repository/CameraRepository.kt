package com.vci.vectorcamapp.imaging.domain.repository

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError
import com.vci.vectorcamapp.core.domain.util.Result

interface CameraRepository {
    suspend fun captureImage(controller: LifecycleCameraController) : Result<ImageProxy, ImagingError>

    suspend fun saveImage(bitmap: Bitmap, filename: String) : Result<Unit, ImagingError>
}