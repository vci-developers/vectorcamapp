package com.vci.vectorcamapp.imaging.presentation.components.camera

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.vci.vectorcamapp.imaging.presentation.enums.CaptureStage
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

/**
 * Static overlay shown while a capture is processing. Inference runs on the GPU and starves the
 * Compose render thread, so any animation here visibly stalls.
 */
@Composable
fun CaptureAnimation(
    modifier: Modifier = Modifier, stage: CaptureStage?
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
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
            modifier = Modifier
                .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusLarge))
                .background(MaterialTheme.colors.cardBackground)
                .padding(MaterialTheme.dimensions.paddingLarge)
        ) {
            Icon(
                painter = painterResource(stage.iconResId),
                contentDescription = null,
                tint = MaterialTheme.colors.secondary,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeExtraExtraLarge)
            )
            Text(
                text = stringResource(stage.labelResId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
