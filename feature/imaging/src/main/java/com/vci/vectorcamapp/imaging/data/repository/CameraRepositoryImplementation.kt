package com.vci.vectorcamapp.imaging.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.core.domain.util.ImagingError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraRepositoryImplementation @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraRepository {
    override suspend fun captureImage(imageCapture: ImageCapture): Result<ImageProxy, ImagingError> {
        return withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            super.onCaptureSuccess(image)
                            continuation.resume(Result.Success(image))
                        }

                        override fun onError(exception: ImageCaptureException) {
                            super.onError(exception)
                            continuation.resume(Result.Error(ImagingError.CAPTURE_ERROR))
                        }
                    })
            }
        }
    }

    override suspend fun saveImage(
        jpegBytes: ByteArray, filename: String, currentSession: Session
    ): Result<Uri, ImagingError> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val sessionTimestamp = dateFormat.format(Date(currentSession.createdAt))

        return withContext(Dispatchers.IO) {
            val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
            val directory = "${Environment.DIRECTORY_DCIM}/$appName/$sessionTimestamp"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val uri = resolver.insert(collection, contentValues) ?: return@withContext Result.Error(
                ImagingError.SAVE_ERROR
            )

            try {
                val outputStream = resolver.openOutputStream(uri)
                if (outputStream == null) {
                    deleteSavedImage(uri)
                    return@withContext Result.Error(ImagingError.SAVE_ERROR)
                }

                val writeSuccess = outputStream.use {
                    it.write(jpegBytes)
                    true
                }

                if (!writeSuccess) {
                    deleteSavedImage(uri)
                    return@withContext Result.Error(ImagingError.SAVE_ERROR)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Result.Success(uri)
            } catch (e: Exception) {
                deleteSavedImage(uri)
                Result.Error(ImagingError.SAVE_ERROR)
            }
        }
    }

    override suspend fun deleteSavedImage(uri: Uri) {
        val resolver = context.contentResolver

        val relativePath = resolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else null
        }

        val isDeleted = resolver.delete(uri, null, null) > 0

        if (isDeleted && relativePath != null) {
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"

            resolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), selection, arrayOf(relativePath), null)?.use { cursor ->
                if (cursor.count == 0) {
                    val folder = File(Environment.getExternalStorageDirectory(), relativePath)
                    if (folder.exists() && folder.isDirectory && folder.listFiles()?.isEmpty() == true) {
                        folder.delete()
                    }
                }
            }
        }
    }
}