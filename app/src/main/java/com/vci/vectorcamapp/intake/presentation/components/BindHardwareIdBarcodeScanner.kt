package com.vci.vectorcamapp.intake.presentation.components

import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.SurfaceRequest
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.vci.vectorcamapp.core.presentation.components.camera.bindBackCamera
import com.vci.vectorcamapp.core.presentation.components.camera.defaultCameraResolutionSelector
import com.vci.vectorcamapp.core.presentation.components.camera.unbindCamera
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean

private const val HARDWARE_ID_MAX_CHARACTERS = 200
private const val HARDWARE_ID_MAX_CHARACTER_CODE = 255

@Composable
internal fun BindHardwareIdBarcodeScanner(
    onBarcodeScanned: (String) -> Unit,
    onSurfaceRequest: (SurfaceRequest) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val rotation = view.display?.rotation ?: Surface.ROTATION_0
    val onBarcodeScannedState = rememberUpdatedState(onBarcodeScanned)
    val onSurfaceRequestState = rememberUpdatedState(onSurfaceRequest)

    LaunchedEffect(lifecycleOwner, rotation) {
        val barcodeScanner = BarcodeScanning.getClient()
        val delivered = AtomicBoolean(false)
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setResolutionSelector(defaultCameraResolutionSelector())
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
                val hardwareId = result.getValue(barcodeScanner)
                    .orEmpty()
                    .firstNotNullOfOrNull { barcode ->
                        val trimmed = barcode.rawValue?.trim().orEmpty()
                        val hasValidCharacters = trimmed.all { character ->
                            character.code <= HARDWARE_ID_MAX_CHARACTER_CODE || character.isWhitespace()
                        }
                        trimmed.takeIf {
                            it.isNotEmpty() &&
                                it.length <= HARDWARE_ID_MAX_CHARACTERS &&
                                hasValidCharacters
                        }
                    }
                if (hardwareId != null && delivered.compareAndSet(false, true)) {
                    onBarcodeScannedState.value(hardwareId)
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
            )
            awaitCancellation()
        } catch (exception: ExecutionException) {
            Timber.e(exception, "Could not open the barcode scanner camera")
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception, "Could not open the barcode scanner camera")
        } catch (exception: IllegalStateException) {
            Timber.e(exception, "Could not open the barcode scanner camera")
        } finally {
            withContext(NonCancellable) {
                imageAnalysis.clearAnalyzer()
                try {
                    unbindCamera(context)
                } finally {
                    barcodeScanner.close()
                }
            }
        }
    }
}
