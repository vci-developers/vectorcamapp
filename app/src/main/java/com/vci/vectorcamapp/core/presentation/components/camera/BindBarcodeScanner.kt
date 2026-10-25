package com.vci.vectorcamapp.core.presentation.components.camera

import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.CameraSelector
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun BindBarcodeScanner(
    onBarcodeScanned: (String) -> Unit,
    onSurfaceRequest: (SurfaceRequest) -> Unit,
    qrOnly: Boolean,
    transformValue: (String) -> String?,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onCameraReleased: () -> Unit = {},
    transformRecognizedText: ((String) -> String?)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val rotation = view.display?.rotation ?: Surface.ROTATION_0
    val onBarcodeScannedState = rememberUpdatedState(onBarcodeScanned)
    val onSurfaceRequestState = rememberUpdatedState(onSurfaceRequest)
    val transformValueState = rememberUpdatedState(transformValue)
    val onCameraReleasedState = rememberUpdatedState(onCameraReleased)
    val transformRecognizedTextState = rememberUpdatedState(transformRecognizedText)
    val recognizeText = transformRecognizedText != null

    LaunchedEffect(lifecycleOwner, rotation, qrOnly, cameraSelector, recognizeText) {
        val options = BarcodeScannerOptions.Builder().apply {
            if (qrOnly) setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        }.build()
        val barcodeScanner = BarcodeScanning.getClient(options)
        val textRecognizer = if (recognizeText) {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        } else null
        val delivered = AtomicBoolean(false)
        val resolutionSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                .build()
        } else {
            defaultCameraResolutionSelector()
        }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setResolutionSelector(resolutionSelector)
            .build()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        imageAnalysis.setAnalyzer(
            mainExecutor,
            MlKitAnalyzer(
                listOfNotNull(barcodeScanner, textRecognizer),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                mainExecutor,
            ) { result ->
                if (delivered.get()) {
                    return@MlKitAnalyzer
                }
                // MlKitAnalyzer completes both detectors for this frame before this callback.
                val barcodeValues = if (result.getThrowable(barcodeScanner) == null) {
                    result.getValue(barcodeScanner).orEmpty().mapNotNull { it.rawValue }
                } else emptyList()
                val text = textRecognizer?.let { recognizer ->
                    if (result.getThrowable(recognizer) == null) result.getValue(recognizer)?.text else null
                }
                val value = selectScanValue(
                    barcodeValues, text, transformValueState.value, transformRecognizedTextState.value
                )
                if (value != null && delivered.compareAndSet(false, true)) {
                    onBarcodeScannedState.value(value)
                }
            },
        )

        try {
            bindBackCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                rotation = rotation,
                onSurfaceRequest = { request -> onSurfaceRequestState.value(request) },
                additionalUseCases = listOf(imageAnalysis),
                cameraSelector = cameraSelector,
            )
            Timber.d("Barcode scanner analysis resolution: %s", imageAnalysis.resolutionInfo?.resolution)
            awaitCancellation()
        } catch (exception: ExecutionException) {
            Timber.e(exception, "Could not open the barcode scanner camera")
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception, "Could not open the barcode scanner camera")
        } catch (exception: IllegalStateException) {
            Timber.e(exception, "Could not open the barcode scanner camera")
        } catch (exception: SecurityException) {
            Timber.e(exception, "Camera permission is unavailable")
        } finally {
            withContext(NonCancellable) {
                imageAnalysis.clearAnalyzer()
                try {
                    unbindCamera(context)
                } finally {
                    barcodeScanner.close()
                    textRecognizer?.close()
                    onCameraReleasedState.value()
                }
            }
        }
    }
}
