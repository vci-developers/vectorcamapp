package com.vci.vectorcamapp.imaging.domain.repository

import android.net.Uri
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.imaging.domain.camera.CameraMetadataListener
import com.vci.vectorcamapp.imaging.domain.model.FocusStackResult
import com.vci.vectorcamapp.imaging.domain.util.ImagingError

interface CameraRepository {
    suspend fun captureImage(
        imageCapture: ImageCapture,
        cameraControl: CameraControl,
        cameraInfo: CameraInfo,
        metadataListener: CameraMetadataListener,
    ): Result<FocusStackResult, ImagingError>
    suspend fun saveImage(jpegBytes: ByteArray, filename: String, currentSession: Session): Result<Uri, ImagingError>
    suspend fun deleteSavedImage(uri: Uri)
}
