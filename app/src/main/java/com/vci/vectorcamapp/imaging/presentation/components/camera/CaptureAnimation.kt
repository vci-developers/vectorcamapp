package com.vci.vectorcamapp.imaging.presentation.components.camera

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.imaging.presentation.enums.CaptureStage
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

/**
 * Overlay shown while a capture is processing. The still is published before GPU inference so it
 * can draw; animated progress would stall once inference occupies the render thread.
 */
@Composable
fun CaptureAnimation(
    modifier: Modifier = Modifier,
    stage: CaptureStage?,
    previewBitmap: Bitmap? = null,
) {
    if (stage == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.overlayColor)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusLarge))
                .background(MaterialTheme.colors.cardBackground)
                .padding(
                    horizontal = MaterialTheme.dimensions.paddingExtraLarge,
                    vertical = MaterialTheme.dimensions.paddingExtraLarge
                )
        ) {
            AnimatedContent(
                targetState = stage to previewBitmap,
                transitionSpec = {
                    (fadeIn(tween(CAPTURE_STAGE_FADE_MILLIS)) togetherWith
                        fadeOut(tween(CAPTURE_STAGE_FADE_MILLIS))).using(null)
                },
                contentAlignment = Alignment.Center,
                label = "captureStage",
            ) { (animatedStage, animatedBitmap) ->
                CaptureStageContent(
                    stage = animatedStage,
                    previewBitmap = animatedBitmap,
                )
            }
        }
    }
}

@Composable
private fun CaptureStageContent(
    stage: CaptureStage,
    previewBitmap: Bitmap?,
) {
    val label = stringResource(stage.labelResId)
    val imageBitmap = remember(previewBitmap) { previewBitmap?.asImageBitmap() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
    ) {
        Box(
            modifier = Modifier.size(width = CapturePreviewWidth, height = CapturePreviewHeight),
            contentAlignment = Alignment.Center,
        ) {
            if (imageBitmap != null && stage != CaptureStage.CAPTURING) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = label,
                    contentScale = if (stage == CaptureStage.CLASSIFYING) {
                        ContentScale.Fit
                    } else {
                        ContentScale.Crop
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall))
                        .background(Color.Black)
                )
            } else {
                Icon(
                    painter = painterResource(stage.iconResId),
                    contentDescription = null,
                    tint = MaterialTheme.colors.secondary,
                    modifier = Modifier.size(MaterialTheme.dimensions.iconSizeExtraExtraLarge)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

private val CapturePreviewWidth = 168.dp
private val CapturePreviewHeight = 120.dp
private const val CAPTURE_STAGE_FADE_MILLIS = 300
