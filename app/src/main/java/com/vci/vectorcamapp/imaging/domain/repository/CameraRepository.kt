package com.vci.vectorcamapp.imaging.domain.repository

import android.net.Uri
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.imaging.domain.model.CapturedImage
import com.vci.vectorcamapp.imaging.domain.util.ImagingError

interface CameraRepository {
    suspend fun captureImage(): Result<CapturedImage, ImagingError>
    suspend fun saveImage(jpegBytes: ByteArray, filename: String, currentSession: Session): Result<Uri, ImagingError>
    suspend fun deleteSavedImage(uri: Uri)
}
