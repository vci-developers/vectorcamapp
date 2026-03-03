package com.vci.vectorcamapp.imaging.presentation.components.camera

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vci.vectorcamapp.animation.presentation.CaptureAnimation
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.imaging.data.camera.Camera2Controller
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun LiveCameraPreview(
    camera2Controller: Camera2Controller?,
    inferenceResults: List<InferenceResult>,
    focusPoint: Offset?,
    onFocusAt: (Offset) -> Unit,
    onCancelFocus: () -> Unit,
    modifier: Modifier = Modifier,
    isManualFocusing: Boolean,
    isProcessing: Boolean,
    onCameraReady: () -> Unit = {}
) {
    val density = LocalDensity.current
    val DOT_RADIUS_DP = 3.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
    ) {
        val containerWidth = with(density) { maxWidth.toPx() }
        val containerHeight = with(density) { maxHeight.toPx() }
        val containerSize = IntSize(containerWidth.toInt(), containerHeight.toInt())

        if (camera2Controller != null) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surface: SurfaceTexture, width: Int, height: Int
                            ) {
                                camera2Controller.open(surface, width, height)
                                onCameraReady()
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture, width: Int, height: Int
                            ) {}

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                camera2Controller.close()
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            inferenceResults.forEach { result ->
                BoundingBoxOverlay(
                    inferenceResult = result,
                    overlaySize = containerSize,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camera2Controller) {
                    detectTapGestures { tapOffsetInPixels ->
                        if (containerSize.width > 0 && containerSize.height > 0) {
                            val normalizedTap = Offset(
                                x = tapOffsetInPixels.x / containerSize.width.toFloat(),
                                y = tapOffsetInPixels.y / containerSize.height.toFloat()
                            )
                            onFocusAt(normalizedTap)
                            camera2Controller?.focusAt(
                                normalizedTap,
                                containerSize.width.toFloat(),
                                containerSize.height.toFloat()
                            )
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
                                camera2Controller?.cancelFocus()
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
