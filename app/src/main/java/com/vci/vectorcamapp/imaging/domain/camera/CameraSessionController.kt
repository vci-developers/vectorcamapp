package com.vci.vectorcamapp.imaging.domain.camera

import android.Manifest
import android.graphics.Bitmap
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.imaging.domain.model.CapturedImage
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CameraSessionController {
    val frames: Flow<Bitmap>
    val isReady: StateFlow<Boolean>

    @RequiresPermission(Manifest.permission.CAMERA)
    suspend fun attachSurface(surface: Surface, displayRotation: DisplayRotation): Result<Unit, ImagingError>
    suspend fun detachSurface()
    suspend fun captureImage(): Result<CapturedImage, ImagingError>

    fun focusAt(normalizedX: Float, normalizedY: Float)
    fun cancelFocus()

    fun release()
}
