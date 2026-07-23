package com.vci.vectorcamapp.core.presentation.components.tutorial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

private val OverlayColor = Color.Black.copy(alpha = 0.72f)
private const val SPOTLIGHT_PADDING_PX = 16f

@Composable
fun SpotlightOverlay(
    spotlightBounds: Rect?,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(spotlightBounds) {
        if (spotlightBounds != null) {
            alpha.animateTo(1f, animationSpec = tween(300, easing = EaseInOut))
        } else {
            alpha.snapTo(0f)
        }
    }

    if (alpha.value == 0f && spotlightBounds == null) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                alpha = alpha.value,
                compositingStrategy = CompositingStrategy.Offscreen
            )
    ) {
        drawRect(color = OverlayColor)

        if (spotlightBounds != null) {
            val paddedBounds = Rect(
                left = spotlightBounds.left - SPOTLIGHT_PADDING_PX,
                top = spotlightBounds.top - SPOTLIGHT_PADDING_PX,
                right = spotlightBounds.right + SPOTLIGHT_PADDING_PX,
                bottom = spotlightBounds.bottom + SPOTLIGHT_PADDING_PX
            )
            drawRoundRect(
                color = Color.Transparent,
                topLeft = paddedBounds.topLeft,
                size = paddedBounds.size,
                cornerRadius = CornerRadius(24f, 24f),
                blendMode = BlendMode.Clear
            )
        }
    }
}
