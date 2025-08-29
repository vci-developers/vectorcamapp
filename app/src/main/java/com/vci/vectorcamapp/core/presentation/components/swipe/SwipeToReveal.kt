package com.vci.vectorcamapp.core.presentation.components.swipe

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.vci.vectorcamapp.ui.extensions.dimensions
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeToReveal(
    backgroundContent: @Composable () -> Unit,
    revealWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val revealWidthPx = with(density) { revealWidth.toPx() }
    val maxOffset = -revealWidthPx

    val horizontalPadding = MaterialTheme.dimensions.paddingMedium
    val verticalPadding = MaterialTheme.dimensions.paddingSmall

    SubcomposeLayout(modifier = modifier) { constraints ->
        val contentPlaceables = subcompose("content") {
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(maxOffset, 0f)
                        },
                        onDragEnd = {
                            offsetX = if (abs(offsetX) > revealWidthPx / 2) maxOffset else 0f
                        }
                    )
                }
            ) {
                content()
            }
        }.map { it.measure(constraints) }

        val contentWidth = contentPlaceables.maxOfOrNull { it.width } ?: 0
        val contentHeight = contentPlaceables.maxOfOrNull { it.height } ?: 0

        val horizontalPaddingPx = with(density) { (horizontalPadding * 2).roundToPx() }
        val verticalPaddingPx = with(density) { (verticalPadding * 2).roundToPx() }

        val cardWidth = contentWidth - horizontalPaddingPx
        val cardHeight = contentHeight - verticalPaddingPx

        val backgroundPlaceables = subcompose("background") {
            backgroundContent()
        }.map {
            it.measure(
                Constraints.fixed(cardWidth, cardHeight)
            )
        }

        layout(contentWidth, contentHeight) {
            backgroundPlaceables.forEach { placeable ->
                placeable.placeRelative(
                    with(density) { horizontalPadding.roundToPx() },
                    with(density) { verticalPadding.roundToPx() }
                )
            }
            contentPlaceables.forEach { placeable ->
                placeable.placeRelative(offsetX.roundToInt(), 0)
            }
        }
    }
}
