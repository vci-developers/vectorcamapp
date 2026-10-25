package com.vci.vectorcamapp.core.presentation.components.camera

import android.util.Size
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
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val rotation = view.display?.rotation ?: Surface.ROTATION_0
    val onBarcodeScannedState = rememberUpdatedState(onBarcodeScanned)
    val onSurfaceRequestState = rememberUpdatedState(onSurfaceRequest)
    val transformValueState = rememberUpdatedState(transformValue)
    val onCameraReleasedState = rememberUpdatedState(onCameraReleased)

    LaunchedEffect(lifecycleOwner, rotation, qrOnly, cameraSelector) {
        val options = BarcodeScannerOptions.Builder().apply {
            if (qrOnly) setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        }.build()
        val barcodeScanner = BarcodeScanning.getClient(options)
        val delivered = AtomicBoolean(false)
        val resolutionSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1920, 1440),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                )
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
                listOf(barcodeScanner),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                mainExecutor,
            ) { result ->
                if (delivered.get() || result.getThrowable(barcodeScanner) != null) {
                    return@MlKitAnalyzer
                }
                val value = result.getValue(barcodeScanner)
                    .orEmpty()
                    .firstNotNullOfOrNull { barcode ->
                        barcode.rawValue?.let { transformValueState.value(it) }
                    }
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
                    onCameraReleasedState.value()
                }
            }
        }
    }
}
