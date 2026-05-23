package com.vci.vectorcamapp.imaging.presentation.components.camera

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.vci.vectorcamapp.animation.presentation.CaptureAnimation
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.imaging.domain.camera.DisplayRotation
import com.vci.vectorcamapp.ui.extensions.colors

@Composable
fun LiveCameraPreview(
    inferenceResults: List<InferenceResult>,
    focusPoint: Offset?,
    onFocusAt: (Offset) -> Unit,
    onCancelFocus: () -> Unit,
    onAttachSurface: (Surface, DisplayRotation) -> Unit,
    onDetachSurface: () -> Unit,
    modifier: Modifier = Modifier,
    isManualFocusing: Boolean,
    isProcessing: Boolean
) {
    val density = LocalDensity.current
    val view = LocalView.current

    val displayRotation = remember(view) {
        when (view.display?.rotation ?: Surface.ROTATION_0) {
            Surface.ROTATION_90 -> DisplayRotation.ROTATION_90
            Surface.ROTATION_180 -> DisplayRotation.ROTATION_180
            Surface.ROTATION_270 -> DisplayRotation.ROTATION_270
            else -> DisplayRotation.ROTATION_0
        }
    }

    BoxWithConstraints(
        modifier = modifier
    ) {
        val containerWidth = with(density) { maxWidth.toPx() }
        val containerHeight = with(density) { maxHeight.toPx() }
        val containerSize = IntSize(containerWidth.toInt(), containerHeight.toInt())

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            onAttachSurface(holder.surface, displayRotation)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder, format: Int, width: Int, height: Int
                        ) = Unit

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            onDetachSurface()
                        }
                    })
                }
            }
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
                            onCancel = { onCancelFocus() }
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

    DisposableEffect(Unit) {
        onDispose { onDetachSurface() }
    }
}

private val DOT_RADIUS_DP = 3.dp
