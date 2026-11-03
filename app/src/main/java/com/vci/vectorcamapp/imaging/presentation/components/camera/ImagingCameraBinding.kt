package com.vci.vectorcamapp.imaging.presentation.components.camera

import android.content.Context
import android.view.Surface
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vci.vectorcamapp.core.presentation.components.camera.bindBackCamera
import com.vci.vectorcamapp.imaging.data.camera.CameraMetadataListenerImplementation
import com.vci.vectorcamapp.imaging.presentation.ImagingAction
import com.vci.vectorcamapp.imaging.presentation.SpecimenImageAnalyzer
import timber.log.Timber

internal class ImagingCameraBinding(
    val surfaceRequest: SurfaceRequest?,
    val imageCapture: ImageCapture?,
    val camera: Camera?,
    val metadataListener: CameraMetadataListenerImplementation,
)

@Composable
internal fun rememberImagingCameraBinding(
    isReviewing: Boolean,
    onAction: (ImagingAction) -> Unit,
): ImagingCameraBinding {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val rotation = view.display?.rotation ?: Surface.ROTATION_0

    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val metadataListener = remember { CameraMetadataListenerImplementation() }
    val reviewing by rememberUpdatedState(isReviewing)
    val analyzer = remember {
        SpecimenImageAnalyzer { frame ->
            if (!reviewing) {
                onAction(ImagingAction.ProcessFrame(frame))
            } else {
                frame.close()
            }
        }
    }

    LaunchedEffect(lifecycleOwner, rotation) {
        val imageCapture = buildImageCapture(rotation, metadataListener)
        val imageAnalysis = buildImageAnalysis(rotation)
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
        bindCameraOrKeepScreen(
            context = context,
            lifecycleOwner = lifecycleOwner,
            rotation = rotation,
            imageCapture = imageCapture,
            imageAnalysis = imageAnalysis,
            onSurfaceRequest = { request -> surfaceRequest = request },
            onBound = { boundCamera ->
                imageCaptureUseCase = imageCapture
                camera = boundCamera
            },
        )
    }

    return ImagingCameraBinding(
        surfaceRequest = surfaceRequest,
        imageCapture = imageCaptureUseCase,
        camera = camera,
        metadataListener = metadataListener,
    )
}

private fun buildImageCapture(
    rotation: Int,
    metadataListener: CameraMetadataListenerImplementation,
): ImageCapture {
    val imageCaptureBuilder = ImageCapture.Builder()
        .setTargetRotation(rotation)
        .setResolutionSelector(ratio4by3Selector())
    Camera2Interop.Extender(imageCaptureBuilder).setSessionCaptureCallback(metadataListener)
    return imageCaptureBuilder.build()
}

private fun buildImageAnalysis(rotation: Int): ImageAnalysis {
    return ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetRotation(rotation)
        .setResolutionSelector(ratio4by3Selector())
        .build()
}

@Suppress("TooGenericExceptionCaught")
private suspend fun bindCameraOrKeepScreen(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    rotation: Int,
    imageCapture: ImageCapture,
    imageAnalysis: ImageAnalysis,
    onSurfaceRequest: (SurfaceRequest) -> Unit,
    onBound: (Camera) -> Unit,
) {
    try {
        val boundCamera = bindBackCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            rotation = rotation,
            onSurfaceRequest = onSurfaceRequest,
            additionalUseCases = listOf<UseCase>(imageCapture, imageAnalysis),
        )
        onBound(boundCamera)
    } catch (e: Exception) {
        Timber.e(e, "Failed to bind the imaging camera")
    }
}

private fun ratio4by3Selector(): ResolutionSelector {
    return ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .build()
}
