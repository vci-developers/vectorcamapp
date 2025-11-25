package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vci.vectorcamapp.animation.presentation.CaptureAnimation
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.imaging.data.camera.CameraFocusControllerImplementation
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun LiveCameraPreview(
    controller: LifecycleCameraController,
    specimenId: String,
    inferenceResults: List<InferenceResult>,
    focusPoint: Offset?,
    onFocusAt: (Offset) -> Unit,
    onCancelFocus: () -> Unit,
    modifier: Modifier = Modifier,
    isManualFocusing: Boolean,
    isProcessing: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val DOT_RADIUS_DP = 3.dp

    val previewView = remember {
        PreviewView(context).apply {
            this.controller = controller
            this.scaleType = PreviewView.ScaleType.FIT_CENTER
            controller.bindToLifecycle(lifecycleOwner)
        }
    }

    val cameraFocusController = remember(previewView, controller) {
        CameraFocusControllerImplementation(
            previewView = previewView, controller = controller
        )
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint == null) {
            cameraFocusController.cancelFocus()
        } else {
            cameraFocusController.focusAt(focusPoint)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
    ) {
        val containerSize = IntSize(
            width = with(density) { maxWidth.roundToPx() },
            height = with(density) { maxHeight.roundToPx() })

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        TrayAlignmentGuideOverlay(
            overlaySize = containerSize,
            hasSpecimenId = specimenId.isNotEmpty(),
            hasSpecimenImage = inferenceResults.isNotEmpty(),
            modifier = Modifier.fillMaxSize()
        )

        inferenceResults.map {
            BoundingBoxOverlay(
                inferenceResult = it,
                overlaySize = containerSize
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffsetInPixels ->
                        if (containerSize.width > 0 && containerSize.height > 0) {
                            val normalizedTap = Offset(
                                x = tapOffsetInPixels.x / containerSize.width.toFloat(),
                                y = tapOffsetInPixels.y / containerSize.height.toFloat()
                            )
                            onFocusAt(normalizedTap)
                        }
                    }
                }
        ) {
            focusPoint?.let { normalized ->
                if (containerSize != IntSize.Zero) {
                    val centerPx = Offset(
                        x = normalized.x * containerSize.width,
                        y = normalized.y * containerSize.height
                    )
                    val dotColor = MaterialTheme.colors.warning

                    if (isManualFocusing || inferenceResults.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = dotColor,
                                radius = DOT_RADIUS_DP.toPx(),
                                center = centerPx
                            )
                        }
                    }

                    if (isManualFocusing) {
                        ManualFocusRingOverlay(
                            focusPoint = centerPx,
                            overlaySize = containerSize,
                            onCancel = {
                                cameraFocusController.cancelFocus()
                                onCancelFocus()
                            }
                        )
                    }
                }
            }
        }

        CaptureAnimation(
            modifier = Modifier.fillMaxSize(),
            isVisible = isProcessing
        )
    }
}
