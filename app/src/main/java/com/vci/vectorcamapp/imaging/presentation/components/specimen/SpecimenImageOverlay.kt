package com.vci.vectorcamapp.imaging.presentation.components.specimen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.imaging.presentation.components.camera.BoundingBoxOverlay
import com.vci.vectorcamapp.ui.extensions.zoomPanGesture

@Composable
fun SpecimenImageOverlay(
    inferenceResult: InferenceResult?,
    modifier: Modifier = Modifier,
    enableZoomPan: Boolean = true,
    showBoundingBoxOverlay: Boolean = true,
    imageContent: @Composable (containerSize: IntSize) -> Unit
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
    ) {
        val containerSize = IntSize(
            width = with(density) { maxWidth.roundToPx() },
            height = with(density) { maxHeight.roundToPx() }
        )

        Box(
            modifier = Modifier
                .clipToBounds()
                .then(
                    if (enableZoomPan) {
                        Modifier.zoomPanGesture(containerSize)
                    } else {
                        Modifier
                    }
                )
        ) {
            imageContent(containerSize)

            if (inferenceResult != null && showBoundingBoxOverlay) {
                BoundingBoxOverlay(
                    inferenceResult = inferenceResult,
                    overlaySize = containerSize
                )
            }
        }
    }
}
