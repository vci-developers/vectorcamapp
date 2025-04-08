package com.vci.vectorcamapp.animation.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun CaptureAnimation(modifier: Modifier = Modifier) {
    val animationDuration = 2000
    val infiniteTransition = rememberInfiniteTransition(label = "capture")

    // Scan animation
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanOffset"
    )

    // Background alpha animation
    val backgroundAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration / 4, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backgroundAlpha"
    )

    // Track direction
    var previousOffset by remember { mutableFloatStateOf(scanOffset) }
    val isGoingDown = scanOffset >= previousOffset
    LaunchedEffect(scanOffset) {
        previousOffset = scanOffset
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerHeight = maxHeight
        val offsetY = (scanOffset * containerHeight.value).dp

        val lineHeight = 8.dp
        val auraHeight = 64.dp
        val lineColor = Color.Green
        val auraColor = Color.Green.copy(alpha = 0.3f)

        // Main scanning line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(lineHeight)
                .offset(y = offsetY)
                .background(lineColor)
                .zIndex(2f)
        )

        // Aura trail
        if (isGoingDown) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(auraHeight)
                    .offset(y = offsetY - auraHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, auraColor)
                        )
                    )
                    .zIndex(1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(auraHeight)
                    .offset(y = offsetY + lineHeight)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(auraColor, Color.Transparent)
                        )
                    )
                    .zIndex(1f)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun CaptureAnimationPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            CaptureAnimation(modifier = Modifier.padding(innerPadding))
        }
    }
}
