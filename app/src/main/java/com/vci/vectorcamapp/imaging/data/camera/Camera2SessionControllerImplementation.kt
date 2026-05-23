package com.vci.vectorcamapp.imaging.data.camera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.imaging.domain.camera.CameraSessionController
import com.vci.vectorcamapp.imaging.domain.camera.DisplayRotation
import com.vci.vectorcamapp.imaging.domain.model.AfRegion
import com.vci.vectorcamapp.imaging.domain.model.CameraMetadata
import com.vci.vectorcamapp.imaging.domain.model.CapturedImage
import com.vci.vectorcamapp.imaging.domain.model.ColorCorrectionGains
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Camera2SessionControllerImplementation @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraSessionController {

    private val sessionLock = Any()
    private var isReleased = false

    private val handlerThread = HandlerThread(HANDLER_THREAD_NAME).apply { start() }
    private val handler = Handler(handlerThread.looper)
    private val handlerExecutor = Executor(handler::post)

    private val analysisHandlerThread = HandlerThread(ANALYSIS_HANDLER_THREAD_NAME).apply { start() }
    private val analysisHandler = Handler(analysisHandlerThread.looper)

    private val cameraManager: CameraManager = context.getSystemService(CameraManager::class.java)
    private val backCameraId: String? = findBackCameraId()
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var analysisImageReader: ImageReader? = null
    private var stillImageReader: ImageReader? = null
    private var previewSurface: Surface? = null

    @Volatile
    private var frameRotationDegrees = 0

    @Volatile
    private var currentDisplayRotation: DisplayRotation = DisplayRotation.ROTATION_0

    private val _frames = MutableSharedFlow<Bitmap>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val frames: Flow<Bitmap> = _frames.asSharedFlow()

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun attachSurface(
        surface: Surface, displayRotation: DisplayRotation
    ): Result<Unit, ImagingError> {
        val cameraId = backCameraId ?: return Result.Error(ImagingError.CAMERA_OPEN_FAILED)

        val device = when (val result = openCameraDevice(cameraId)) {
            is Result.Error -> return Result.Error(result.error)
            is Result.Success -> result.data
        }

        val session = when (val result = configureCaptureSession(device, surface)) {
            is Result.Error -> {
                detachSurface()
                return Result.Error(result.error)
            }
            is Result.Success -> result.data
        }

        synchronized(sessionLock) {
            previewSurface = surface
            currentDisplayRotation = displayRotation
        }
        frameRotationDegrees = computeFrameRotation(cameraId, displayRotation)

        when (val result = startPreview(session, surface)) {
            is Result.Error -> {
                detachSurface()
                return Result.Error(result.error)
            }
            is Result.Success -> Unit
        }

        return Result.Success(Unit)
    }

    override suspend fun detachSurface() {
        synchronized(sessionLock) {
            captureSession?.close()
            captureSession = null
            analysisImageReader?.close()
            analysisImageReader = null
            stillImageReader?.close()
            stillImageReader = null
            cameraDevice?.close()
            cameraDevice = null
            previewSurface = null
            _isReady.value = false
        }
    }

    override suspend fun captureImage(): Result<CapturedImage, ImagingError> {
        val session = synchronized(sessionLock) { captureSession }
            ?: return Result.Error(ImagingError.CAPTURE_ERROR)
        val device = synchronized(sessionLock) { cameraDevice }
            ?: return Result.Error(ImagingError.CAPTURE_ERROR)
        val stillReader = synchronized(sessionLock) { stillImageReader }
            ?: return Result.Error(ImagingError.CAPTURE_ERROR)

        return suspendCancellableCoroutine { continuation ->
            val captureMutex = Any()
            var capturedJpegBytes: ByteArray? = null
            var capturedMetadata: CameraMetadata? = null
            var resumedAlready = false

            fun tryComplete() {
                synchronized(captureMutex) {
                    if (resumedAlready) return
                    val bytes = capturedJpegBytes
                    val metadata = capturedMetadata
                    if (bytes != null && metadata != null) {
                        resumedAlready = true
                        val (effectiveWidth, effectiveHeight) = when (frameRotationDegrees) {
                            ROTATION_90_DEGREES, ROTATION_270_DEGREES ->
                                stillReader.height to stillReader.width
                            else -> stillReader.width to stillReader.height
                        }
                        val finalMetadata = metadata.copy(
                            imageWidth = effectiveWidth,
                            imageHeight = effectiveHeight
                        )
                        if (continuation.isActive) {
                            continuation.resume(
                                Result.Success(CapturedImage(bytes, finalMetadata))
                            )
                        }
                    }
                }
            }

            fun fail(error: ImagingError) {
                synchronized(captureMutex) {
                    if (resumedAlready) return
                    resumedAlready = true
                    if (continuation.isActive) {
                        continuation.resume(Result.Error(error))
                    }
                }
            }

            stillReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val bitmap = image.toUprightBitmap(frameRotationDegrees)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
                    synchronized(captureMutex) { capturedJpegBytes = stream.toByteArray() }
                    tryComplete()
                } catch (e: Exception) {
                    fail(ImagingError.CAPTURE_ERROR)
                } finally {
                    image.close()
                }
            }, handler)

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    synchronized(captureMutex) { capturedMetadata = extractCameraMetadata(result) }
                    tryComplete()
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    fail(ImagingError.CAPTURE_ERROR)
                }
            }

            try {
                val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(stillReader.surface)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                }.build()
                session.capture(request, captureCallback, handler)
            } catch (e: Exception) {
                fail(ImagingError.CAPTURE_ERROR)
            }
        }
    }

    override fun focusAt(normalizedX: Float, normalizedY: Float) {
        val cameraId = backCameraId ?: return
        val session = synchronized(sessionLock) { captureSession } ?: return
        val device = synchronized(sessionLock) { cameraDevice } ?: return
        val analysisReader = synchronized(sessionLock) { analysisImageReader } ?: return
        val preview = synchronized(sessionLock) { previewSurface } ?: return
        val displayRotation = synchronized(sessionLock) { currentDisplayRotation }

        val sensorRect = computeSensorMeteringRect(cameraId, normalizedX, normalizedY, displayRotation)
            ?: return
        val meteringRectangle = MeteringRectangle(sensorRect, METERING_WEIGHT_MAX)
        val regions = arrayOf(meteringRectangle)

        try {
            val repeatingRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(preview)
                addTarget(analysisReader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_REGIONS, regions)
                set(CaptureRequest.CONTROL_AE_REGIONS, regions)
            }.build()
            session.setRepeatingRequest(repeatingRequest, null, handler)

            val triggerRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(preview)
                addTarget(analysisReader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_REGIONS, regions)
                set(CaptureRequest.CONTROL_AE_REGIONS, regions)
                set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
            }.build()
            session.capture(triggerRequest, null, handler)
        } catch (_: Exception) {
        }
    }

    override fun cancelFocus() {
        val session = synchronized(sessionLock) { captureSession } ?: return
        val device = synchronized(sessionLock) { cameraDevice } ?: return
        val analysisReader = synchronized(sessionLock) { analysisImageReader } ?: return
        val preview = synchronized(sessionLock) { previewSurface } ?: return

        try {
            val cancelRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(preview)
                addTarget(analysisReader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
            }.build()
            session.capture(cancelRequest, null, handler)

            val resumeRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(preview)
                addTarget(analysisReader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }.build()
            session.setRepeatingRequest(resumeRequest, null, handler)
        } catch (_: Exception) {
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun openCameraDevice(cameraId: String): Result<CameraDevice, ImagingError> {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                synchronized(sessionLock) {
                    cameraDevice?.close()
                    cameraDevice = null
                    _isReady.value = false
                }
            }

            val callback = object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    val opened = synchronized(sessionLock) {
                        if (isReleased) {
                            device.close()
                            false
                        } else {
                            cameraDevice = device
                            true
                        }
                    }
                    if (continuation.isActive) {
                        continuation.resume(
                            if (opened) Result.Success(device)
                            else Result.Error(ImagingError.CAMERA_OPEN_FAILED)
                        )
                    }
                }

                override fun onDisconnected(device: CameraDevice) {
                    handleDeviceFailure(device)
                    if (continuation.isActive) {
                        continuation.resume(Result.Error(ImagingError.CAMERA_OPEN_FAILED))
                    }
                }

                override fun onError(device: CameraDevice, error: Int) {
                    handleDeviceFailure(device)
                    if (continuation.isActive) {
                        continuation.resume(Result.Error(ImagingError.CAMERA_OPEN_FAILED))
                    }
                }
            }

            try {
                cameraManager.openCamera(cameraId, callback, handler)
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.Error(ImagingError.CAMERA_OPEN_FAILED))
                }
            }
        }
    }

    private suspend fun configureCaptureSession(
        device: CameraDevice, previewSurface: Surface
    ): Result<CameraCaptureSession, ImagingError> {
        val map = try {
            cameraManager.getCameraCharacteristics(device.id)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        } catch (e: CameraAccessException) {
            null
        } ?: return Result.Error(ImagingError.SESSION_CONFIG_FAILED)

        val analysisSize = pickAnalysisSize(map) ?: return Result.Error(ImagingError.SESSION_CONFIG_FAILED)
        val stillSize = pickStillSize(map) ?: return Result.Error(ImagingError.SESSION_CONFIG_FAILED)

        val analysisReader = ImageReader.newInstance(
            analysisSize.width, analysisSize.height, ImageFormat.YUV_420_888, MAX_ANALYSIS_IMAGES
        )
        val stillReader = ImageReader.newInstance(
            stillSize.width, stillSize.height, ImageFormat.YUV_420_888, MAX_STILL_IMAGES
        )

        synchronized(sessionLock) {
            analysisImageReader = analysisReader
            stillImageReader = stillReader
        }

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                synchronized(sessionLock) {
                    captureSession?.close()
                    captureSession = null
                    _isReady.value = false
                }
            }

            val callback = object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    val configured = synchronized(sessionLock) {
                        if (isReleased) {
                            session.close()
                            false
                        } else {
                            captureSession = session
                            true
                        }
                    }
                    if (continuation.isActive) {
                        continuation.resume(
                            if (configured) Result.Success(session)
                            else Result.Error(ImagingError.SESSION_CONFIG_FAILED)
                        )
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    synchronized(sessionLock) {
                        session.close()
                        if (captureSession == session) {
                            captureSession = null
                        }
                    }
                    if (continuation.isActive) {
                        continuation.resume(Result.Error(ImagingError.SESSION_CONFIG_FAILED))
                    }
                }
            }

            val outputConfigs = listOf(
                OutputConfiguration(previewSurface),
                OutputConfiguration(analysisReader.surface),
                OutputConfiguration(stillReader.surface)
            )
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR, outputConfigs, handlerExecutor, callback
            )

            try {
                device.createCaptureSession(sessionConfig)
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.Error(ImagingError.SESSION_CONFIG_FAILED))
                }
            }
        }
    }

    private fun startPreview(
        session: CameraCaptureSession, previewSurface: Surface
    ): Result<Unit, ImagingError> {
        val analysisReader = synchronized(sessionLock) { analysisImageReader }
            ?: return Result.Error(ImagingError.SESSION_CONFIG_FAILED)
        val device = synchronized(sessionLock) { cameraDevice }
            ?: return Result.Error(ImagingError.SESSION_CONFIG_FAILED)

        analysisReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val bitmap = image.toUprightBitmap(frameRotationDegrees)
                _frames.tryEmit(bitmap)
            } catch (_: Exception) {
            } finally {
                image.close()
            }
        }, analysisHandler)

        return try {
            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                addTarget(analysisReader.surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }.build()
            session.setRepeatingRequest(request, null, handler)
            _isReady.value = true
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ImagingError.SESSION_CONFIG_FAILED)
        }
    }

    private fun pickAnalysisSize(map: StreamConfigurationMap): Size? {
        val sizes = map.getOutputSizes(ImageFormat.YUV_420_888) ?: return null
        val fourThreeSizes = sizes.filter(::isFourThree).ifEmpty { sizes.toList() }
        val targetArea = ANALYSIS_TARGET_WIDTH * ANALYSIS_TARGET_HEIGHT
        return fourThreeSizes.minByOrNull { abs(it.width * it.height - targetArea) }
    }

    private fun pickStillSize(map: StreamConfigurationMap): Size? {
        val regular = map.getOutputSizes(ImageFormat.YUV_420_888) ?: emptyArray()
        val highRes = map.getHighResolutionOutputSizes(ImageFormat.YUV_420_888) ?: emptyArray()
        val combined = (regular + highRes).toList()
        if (combined.isEmpty()) return null
        val fourThreeSizes = combined.filter(::isFourThree).ifEmpty { combined }
        return fourThreeSizes.maxByOrNull { it.width * it.height }
    }

    private fun isFourThree(size: Size): Boolean =
        abs(size.width.toFloat() / size.height - FOUR_THREE_RATIO) < FOUR_THREE_RATIO_TOLERANCE

    private fun computeFrameRotation(cameraId: String, displayRotation: DisplayRotation): Int {
        val sensorOrientation = try {
            cameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        } catch (e: CameraAccessException) {
            0
        }
        return (sensorOrientation - displayRotation.degrees + FULL_ROTATION_DEGREES) % FULL_ROTATION_DEGREES
    }

    private fun computeSensorMeteringRect(
        cameraId: String, normalizedX: Float, normalizedY: Float, displayRotation: DisplayRotation
    ): Rect? {
        val characteristics = try {
            cameraManager.getCameraCharacteristics(cameraId)
        } catch (e: CameraAccessException) {
            return null
        }
        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            ?: return null
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

        val clampedX = normalizedX.coerceIn(0f, 1f)
        val clampedY = normalizedY.coerceIn(0f, 1f)
        val rotationDegrees = (sensorOrientation - displayRotation.degrees + FULL_ROTATION_DEGREES) % FULL_ROTATION_DEGREES

        val (sensorXNormalized, sensorYNormalized) = when (rotationDegrees) {
            ROTATION_90_DEGREES -> clampedY to (1f - clampedX)
            ROTATION_180_DEGREES -> (1f - clampedX) to (1f - clampedY)
            ROTATION_270_DEGREES -> (1f - clampedY) to clampedX
            else -> clampedX to clampedY
        }

        val sensorWidth = activeArray.width()
        val sensorHeight = activeArray.height()
        val regionHalfWidth = (sensorWidth * METERING_REGION_RELATIVE_SIZE / 2f).toInt()
        val regionHalfHeight = (sensorHeight * METERING_REGION_RELATIVE_SIZE / 2f).toInt()
        val centerX = (sensorXNormalized * sensorWidth).toInt() + activeArray.left
        val centerY = (sensorYNormalized * sensorHeight).toInt() + activeArray.top

        val left = max(activeArray.left, centerX - regionHalfWidth)
        val top = max(activeArray.top, centerY - regionHalfHeight)
        val right = min(activeArray.right, centerX + regionHalfWidth)
        val bottom = min(activeArray.bottom, centerY + regionHalfHeight)

        return Rect(left, top, right, bottom)
    }

    private fun extractCameraMetadata(result: TotalCaptureResult): CameraMetadata {
        val rggb = result[CaptureResult.COLOR_CORRECTION_GAINS]
        return CameraMetadata(
            focusDistance = result[CaptureResult.LENS_FOCUS_DISTANCE],
            aperture = result[CaptureResult.LENS_APERTURE],
            exposureTimeNs = result[CaptureResult.SENSOR_EXPOSURE_TIME],
            iso = result[CaptureResult.SENSOR_SENSITIVITY],
            colorCorrectionGains = rggb?.let {
                ColorCorrectionGains(
                    red = it.red,
                    greenEven = it.greenEven,
                    greenOdd = it.greenOdd,
                    blue = it.blue
                )
            },
            awbMode = result[CaptureResult.CONTROL_AWB_MODE],
            afRegions = result[CaptureResult.CONTROL_AF_REGIONS]?.map { region ->
                AfRegion(
                    x = region.x,
                    y = region.y,
                    width = region.width,
                    height = region.height,
                    weight = region.meteringWeight
                )
            } ?: emptyList()
        )
    }

    private fun findBackCameraId(): String? = try {
        cameraManager.cameraIdList
            .filter { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
            .maxByOrNull { id ->
                val map = cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                map?.getOutputSizes(ImageFormat.YUV_420_888)
                    ?.filter(::isFourThree)
                    ?.maxOfOrNull { it.width * it.height }
                    ?: 0
            }
    } catch (e: CameraAccessException) {
        null
    }

    private fun handleDeviceFailure(device: CameraDevice) {
        synchronized(sessionLock) {
            device.close()
            if (cameraDevice == device) {
                cameraDevice = null
                _isReady.value = false
            }
        }
    }

    override fun release() {
        synchronized(sessionLock) {
            if (isReleased) return
            isReleased = true
            captureSession?.close()
            captureSession = null
            analysisImageReader?.close()
            analysisImageReader = null
            stillImageReader?.close()
            stillImageReader = null
            cameraDevice?.close()
            cameraDevice = null
            previewSurface = null
            _isReady.value = false
            handlerThread.quitSafely()
            analysisHandlerThread.quitSafely()
        }
    }

    private companion object {
        private const val HANDLER_THREAD_NAME = "Camera2SessionThread"
        private const val ANALYSIS_HANDLER_THREAD_NAME = "Camera2AnalysisThread"

        private const val ANALYSIS_TARGET_WIDTH = 640
        private const val ANALYSIS_TARGET_HEIGHT = 480
        private const val MAX_ANALYSIS_IMAGES = 2
        private const val MAX_STILL_IMAGES = 2

        private const val FOUR_THREE_RATIO = 4f / 3f
        private const val FOUR_THREE_RATIO_TOLERANCE = 0.02f

        private const val FULL_ROTATION_DEGREES = 360
        private const val ROTATION_90_DEGREES = 90
        private const val ROTATION_180_DEGREES = 180
        private const val ROTATION_270_DEGREES = 270

        private const val JPEG_QUALITY = 100
        private const val METERING_WEIGHT_MAX = MeteringRectangle.METERING_WEIGHT_MAX
        private const val METERING_REGION_RELATIVE_SIZE = 0.15f
    }
}
