package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.core.ImplementationMode
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.animation.presentation.CaptureAnimation
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.imaging.data.camera.CameraFocusControllerImplementation
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun LiveCameraPreview(
    surfaceRequest: SurfaceRequest?,
    camera: Camera?,
    inferenceResults: List<InferenceResult>,
    focusPoint: Offset?,
    onFocusAt: (Offset) -> Unit,
    onCancelFocus: () -> Unit,
    modifier: Modifier = Modifier,
    isManualFocusing: Boolean,
    isProcessing: Boolean
) {
    val density = LocalDensity.current
    val view = LocalView.current

    val DOT_RADIUS_DP = 3.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
    ) {
        val containerWidth = with(density) { maxWidth.toPx() }
        val containerHeight = with(density) { maxHeight.toPx() }
        val containerSize = IntSize(containerWidth.toInt(), containerHeight.toInt())

        val cameraFocusController = remember(camera, containerWidth, containerHeight) {
            CameraFocusControllerImplementation(
                cameraControl = camera?.cameraControl,
                cameraInfo = camera?.cameraInfo,
                display = view.display,
                width = containerWidth,
                height = containerHeight
            )
        }

        LaunchedEffect(focusPoint) {
            if (focusPoint == null) {
                cameraFocusController.cancelFocus()
            } else {
                cameraFocusController.focusAt(focusPoint)
            }
        }

        if (surfaceRequest != null) {
            CameraXViewfinder(
                surfaceRequest = surfaceRequest,
                implementationMode = ImplementationMode.EXTERNAL,
                modifier = Modifier.fillMaxSize()
            )
        }

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
