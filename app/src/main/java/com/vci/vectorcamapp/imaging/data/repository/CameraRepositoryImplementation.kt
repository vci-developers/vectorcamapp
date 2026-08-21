package com.vci.vectorcamapp.imaging.data.repository

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraMetadata as Camera2Metadata
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.imaging.data.util.FocusStackFusion
import com.vci.vectorcamapp.imaging.domain.camera.CameraMetadataListener
import com.vci.vectorcamapp.imaging.domain.model.FocusStackResult
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfInt
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraRepositoryImplementation @Inject constructor(
    @ApplicationContext private val context: Context,
    private val focusStackFusion: FocusStackFusion,
) : CameraRepository {

    override suspend fun captureImage(
        imageCapture: ImageCapture,
        cameraControl: CameraControl,
        cameraInfo: CameraInfo,
        metadataListener: CameraMetadataListener,
    ): Result<FocusStackResult, ImagingError> = withContext(Dispatchers.Default) {
        val camera2Control = Camera2CameraControl.from(cameraControl)
        val minimumFocusDistance = Camera2CameraInfo.from(cameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        if (minimumFocusDistance == null || minimumFocusDistance <= 0f) {
            return@withContext Result.Error(ImagingError.CAPTURE_ERROR)
        }

        val bracketResult = coroutineScope {
            val processingJobs = mutableListOf<Deferred<Pair<Mat, ByteArray>>>()
            var captureFailed = false
            try {
                for (plane in FOCUS_PLANE_DIOPTERS) {
                    metadataListener.reset()
                    setManualFocus(camera2Control, plane)
                    awaitLensSettled(metadataListener)
                    val imageProxy = takePictureOnce(imageCapture)
                    if (imageProxy == null) {
                        captureFailed = true
                        break
                    }
                    processingJobs += async {
                        val rotatedMat = try {
                            rotateImageProxyToMat(imageProxy)
                        } finally {
                            imageProxy.close()
                        }
                        val jpeg = encodeMatToJpeg(rotatedMat)
                        rotatedMat to jpeg
                    }
                }
            } finally {
                restoreContinuousAutoFocus(camera2Control)
            }

            if (captureFailed) {
                processingJobs.forEach { it.cancel() }
                processingJobs.forEach { job ->
                    runCatching { job.await() }.getOrNull()?.first?.release()
                }
                null
            } else {
                val results = processingJobs.awaitAll()
                results.map { it.first } to results.map { it.second }
            }
        }
        if (bracketResult == null) {
            return@withContext Result.Error(ImagingError.CAPTURE_ERROR)
        }
        val (rotatedMats, frameJpegs) = bracketResult

        val fusion = focusStackFusion.fuse(rotatedMats)
        Result.Success(
            FocusStackResult(
                compositeJpeg = fusion.jpeg,
                frameJpegs = frameJpegs,
                width = fusion.width,
                height = fusion.height,
            )
        )
    }

    override suspend fun saveImage(
        jpegBytes: ByteArray, filename: String, currentSession: Session
    ): Result<Uri, ImagingError> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val sessionTimestamp = dateFormat.format(Date(currentSession.createdAt))

        return withContext(Dispatchers.IO) {
            val appName = context.getString(R.string.media_directory_name)
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

    private suspend fun setManualFocus(
        camera2Control: Camera2CameraControl,
        focusDistance: Float,
    ) {
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, Camera2Metadata.CONTROL_AF_MODE_OFF)
            .setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance)
            .build()
        camera2Control.setCaptureRequestOptions(options).awaitCompletion()
    }

    private suspend fun restoreContinuousAutoFocus(camera2Control: Camera2CameraControl) {
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                Camera2Metadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            .build()
        try {
            camera2Control.setCaptureRequestOptions(options).awaitCompletion()
        } catch (_: Exception) {
        }
    }

    private suspend fun awaitLensSettled(metadataListener: CameraMetadataListener) {
        withTimeoutOrNull(LENS_MAX_SETTLE_MS) {
            while (metadataListener.latestMetadata?.lensState != Camera2Metadata.LENS_STATE_STATIONARY) {
                delay(LENS_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun takePictureOnce(imageCapture: ImageCapture): ImageProxy? =
        withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            super.onCaptureSuccess(image)
                            continuation.resume(image)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            super.onError(exception)
                            continuation.resume(null)
                        }
                    },
                )
            }
        }

    private fun rotateImageProxyToMat(imageProxy: ImageProxy): Mat {
        val buffer = imageProxy.planes[0].buffer
        val sourceBytes = ByteArray(buffer.remaining())
        buffer.get(sourceBytes)

        val source = Imgcodecs.imdecode(
            MatOfByte(*sourceBytes),
            Imgcodecs.IMREAD_COLOR or Imgcodecs.IMREAD_IGNORE_ORIENTATION,
        )
        val destination = Mat()
        when (imageProxy.imageInfo.rotationDegrees) {
            90 -> Core.rotate(source, destination, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(source, destination, Core.ROTATE_180)
            270 -> Core.rotate(source, destination, Core.ROTATE_90_COUNTERCLOCKWISE)
            else -> source.copyTo(destination)
        }
        source.release()
        return destination
    }

    private fun encodeMatToJpeg(mat: Mat): ByteArray {
        val jpegOut = MatOfByte()
        val jpegParams = MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, FRAME_JPEG_QUALITY)
        Imgcodecs.imencode(".jpg", mat, jpegOut, jpegParams)
        val jpegBytes = jpegOut.toArray()
        jpegOut.release()
        jpegParams.release()
        return jpegBytes
    }

    private suspend fun ListenableFuture<Void>.awaitCompletion() =
        suspendCancellableCoroutine<Unit> { continuation ->
            addListener({
                try {
                    get()
                    continuation.resume(Unit)
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }, Runnable::run)
            continuation.invokeOnCancellation { cancel(false) }
        }

    companion object {
        private val FOCUS_PLANE_DIOPTERS = listOf(0f, 10f, 20f)
        private const val LENS_MAX_SETTLE_MS = 200L
        private const val LENS_POLL_INTERVAL_MS = 10L
        private const val FRAME_JPEG_QUALITY = 100
    }
}
