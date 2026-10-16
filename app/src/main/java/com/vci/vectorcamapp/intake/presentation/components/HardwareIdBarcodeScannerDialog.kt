package com.vci.vectorcamapp.intake.presentation.components

import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.SurfaceRequest
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.camera.CameraPreview
import com.vci.vectorcamapp.core.presentation.components.camera.bindBackCamera
import com.vci.vectorcamapp.core.presentation.components.camera.defaultCameraResolutionSelector
import com.vci.vectorcamapp.core.presentation.components.camera.unbindCamera
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean

private const val HARDWARE_ID_MAX_CHARACTERS = 200
private const val HARDWARE_ID_MAX_CHARACTER_CODE = 255

@Composable
fun HardwareIdBarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    BindHardwareIdBarcodeScanner(
        onBarcodeScanned = onBarcodeScanned,
        onSurfaceRequest = { request -> surfaceRequest = request },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .padding(MaterialTheme.dimensions.paddingMedium)
                .fillMaxWidth(),
            shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
            color = MaterialTheme.colors.cardBackground,
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.intake_title_scan_hardware_id),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.intake_content_description_close_scanner),
                            tint = MaterialTheme.colors.icon,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.intake_body_scan_hardware_id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.textSecondary,
                )

                CameraPreview(
                    surfaceRequest = surfaceRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
                        .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall))
                        .background(Color.Black),
                    overlay = {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val inset = MaterialTheme.dimensions.paddingLarge
                            val side = minOf(maxWidth, maxHeight) - (inset * 2)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(side)
                                    .border(
                                        width = MaterialTheme.dimensions.borderThicknessThick,
                                        color = MaterialTheme.colors.buttonText,
                                        shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall),
                                    )
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BindHardwareIdBarcodeScanner(
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
                    .firstNotNullOfOrNull { barcode -> barcode.rawValue?.acceptableHardwareId() }
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

private fun String.acceptableHardwareId(): String? {
    val trimmed = trim()
    val hasValidCharacters = trimmed.all { character ->
        character.code <= HARDWARE_ID_MAX_CHARACTER_CODE || character.isWhitespace()
    }
    return trimmed.takeIf {
        it.isNotEmpty() && it.length <= HARDWARE_ID_MAX_CHARACTERS && hasValidCharacters
    }
}
