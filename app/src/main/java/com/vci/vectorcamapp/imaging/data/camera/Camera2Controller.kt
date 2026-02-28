package com.vci.vectorcamapp.imaging.data.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

/**
 * Manages the full Camera2 lifecycle: preview, analysis (frame callbacks), still capture, and focus.
 * A single camera device and session are shared across preview + analysis + capture.
 */
class Camera2Controller(private val context: Context) {

    companion object {
        private const val TAG = "Camera2Controller"
        private const val ASPECT_RATIO = 4.0 / 3.0
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var previewSurface: Surface? = null
    private var analysisSurface: Surface? = null

    private var analysisImageReader: ImageReader? = null
    private var captureImageReader: ImageReader? = null

    private val cameraThread = HandlerThread("Camera2Thread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    private var cameraId: String? = null
    private var sensorOrientation: Int = 0
    private var previewSize: Size = Size(640, 480)
    private var captureSize: Size = Size(640, 480)

    var onAnalysisFrame: ((Bitmap, Int) -> Unit)? = null

    val isOpen: Boolean get() = cameraDevice != null

    fun getSensorOrientation(): Int = sensorOrientation

    @SuppressLint("MissingPermission")
    fun open(surfaceTexture: SurfaceTexture, previewWidth: Int, previewHeight: Int) {
        val camId = findBackCamera() ?: run {
            Log.e(TAG, "No back camera found")
            return
        }
        cameraId = camId

        val characteristics = cameraManager.getCameraCharacteristics(camId)
        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

        val jpegSizes = map.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
        captureSize = chooseOptimalSize(jpegSizes, 4032, 3024) ?: Size(4032, 3024)

        val yuvSizes = map.getOutputSizes(ImageFormat.YUV_420_888) ?: emptyArray()
        val analysisSize = chooseOptimalSize(yuvSizes, 640, 480) ?: Size(640, 480)

        previewSize = chooseOptimalSize(
            map.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray(),
            previewWidth, previewHeight
        ) ?: Size(previewWidth, previewHeight)

        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        previewSurface = Surface(surfaceTexture)

        analysisImageReader = ImageReader.newInstance(
            analysisSize.width, analysisSize.height, ImageFormat.YUV_420_888, 2
        ).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val bitmap = yuvImageToBitmap(image)
                    onAnalysisFrame?.invoke(bitmap, sensorOrientation)
                } finally {
                    image.close()
                }
            }, cameraHandler)
        }
        analysisSurface = analysisImageReader!!.surface

        captureImageReader = ImageReader.newInstance(
            captureSize.width, captureSize.height, ImageFormat.JPEG, 1
        )

        cameraManager.openCamera(camId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.w(TAG, "Camera disconnected")
                close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e(TAG, "Camera error: $error")
                close()
            }
        }, cameraHandler)
    }

    private fun createSession() {
        val device = cameraDevice ?: return
        val preview = previewSurface ?: return
        val analysis = analysisSurface ?: return
        val capture = captureImageReader?.surface ?: return

        val surfaces = listOf(preview, analysis, capture)

        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e(TAG, "Session configuration failed")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val outputConfigs = surfaces.map { OutputConfiguration(it) }
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigs,
                cameraHandler::post,
                stateCallback
            )
            device.createCaptureSession(sessionConfig)
        } else {
            @Suppress("DEPRECATION")
            device.createCaptureSession(surfaces, stateCallback, cameraHandler)
        }
    }

    private fun startPreview() {
        val session = captureSession ?: return
        val device = cameraDevice ?: return
        val preview = previewSurface ?: return
        val analysis = analysisSurface ?: return

        previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(preview)
            addTarget(analysis)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }

        session.setRepeatingRequest(previewRequestBuilder!!.build(), null, cameraHandler)
    }

    /**
     * Capture a still JPEG and return the raw JPEG bytes.
     * JPEG_ORIENTATION is set so most hardware rotates the pixel data, but callers
     * should still check EXIF to handle devices that only set the metadata tag.
     */
    suspend fun captureStillImage(): ByteArray? = suspendCancellableCoroutine { cont ->
        val session = captureSession
        val device = cameraDevice
        val reader = captureImageReader

        if (session == null || device == null || reader == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        reader.setOnImageAvailableListener({ imgReader ->
            val image = imgReader.acquireNextImage() ?: return@setOnImageAvailableListener
            try {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                if (!cont.isCompleted) cont.resume(bytes)
            } finally {
                image.close()
            }
        }, cameraHandler)

        val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(reader.surface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation)
            set(CaptureRequest.JPEG_QUALITY, 100.toByte())
        }

        session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureFailed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                failure: android.hardware.camera2.CaptureFailure
            ) {
                Log.e(TAG, "Still capture failed")
                if (!cont.isCompleted) cont.resume(null)
            }
        }, cameraHandler)
    }

    fun focusAt(normalizedOffset: Offset, viewWidth: Float, viewHeight: Float) {
        val session = captureSession ?: return
        val builder = previewRequestBuilder ?: return

        val clampedX = min(1f, max(0f, normalizedOffset.x))
        val clampedY = min(1f, max(0f, normalizedOffset.y))

        val meteringX = (clampedX * 1000).toInt().coerceIn(0, 999)
        val meteringY = (clampedY * 1000).toInt().coerceIn(0, 999)
        val halfSize = 50
        val rect = android.graphics.Rect(
            max(0, meteringX - halfSize),
            max(0, meteringY - halfSize),
            min(999, meteringX + halfSize),
            min(999, meteringY + halfSize)
        )

        builder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(MeteringRectangle(rect, MeteringRectangle.METERING_WEIGHT_MAX)))
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(MeteringRectangle(rect, MeteringRectangle.METERING_WEIGHT_MAX)))
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)

        session.capture(builder.build(), object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
                try {
                    session.setRepeatingRequest(builder.build(), null, cameraHandler)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to resume preview after focus", e)
                }
            }
        }, cameraHandler)
    }

    fun cancelFocus() {
        val session = captureSession ?: return
        val builder = previewRequestBuilder ?: return

        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, null)
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, null)

        try {
            session.capture(builder.build(), null, cameraHandler)
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
            session.setRepeatingRequest(builder.build(), null, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel focus", e)
        }
    }

    fun close() {
        try { captureSession?.close() } catch (_: Exception) {}
        try { cameraDevice?.close() } catch (_: Exception) {}
        try { analysisImageReader?.close() } catch (_: Exception) {}
        try { captureImageReader?.close() } catch (_: Exception) {}
        try { previewSurface?.release() } catch (_: Exception) {}
        captureSession = null
        cameraDevice = null
        analysisImageReader = null
        captureImageReader = null
        previewSurface = null
        analysisSurface = null
    }

    fun release() {
        close()
        cameraThread.quitSafely()
    }

    private fun findBackCamera(): String? {
        return cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }

    private fun chooseOptimalSize(sizes: Array<Size>, targetWidth: Int, targetHeight: Int): Size? {
        val targetRatio = ASPECT_RATIO
        return sizes
            .filter { it.width.toDouble() / it.height.toDouble() in (targetRatio - 0.1)..(targetRatio + 0.1) }
            .minByOrNull { kotlin.math.abs(it.width * it.height - targetWidth * targetHeight) }
            ?: sizes.minByOrNull { kotlin.math.abs(it.width * it.height - targetWidth * targetHeight) }
    }

    private fun yuvImageToBitmap(image: Image): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21, android.graphics.ImageFormat.NV21,
            image.width, image.height, null
        )
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 85, out)
        val jpegBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }
}
