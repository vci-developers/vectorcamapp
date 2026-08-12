package com.vci.vectorcamapp.imaging.presentation.components.camera

import androidx.camera.core.Camera
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun FlashToggleOverlay(
    camera: Camera?,
    isFlashOn: Boolean,
    onToggleFlash: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (camera?.cameraInfo?.hasFlashUnit() != true) return

    LaunchedEffect(camera, isFlashOn) {
        camera.cameraControl.enableTorch(isFlashOn)
    }

    DisposableEffect(camera) {
        onDispose {
            camera.cameraControl.enableTorch(false)
        }
    }

    IconButton(
        onClick = { onToggleFlash(!isFlashOn) },
        modifier = modifier.size(MaterialTheme.dimensions.componentHeightMedium),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(MaterialTheme.dimensions.componentHeightSmall)
                .background(
                    color = MaterialTheme.colors.overlayColor,
                    shape = CircleShape
                )
        ) {
            Icon(
                painter = painterResource(
                    id = if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
                ),
                contentDescription = stringResource(
                    id = if (isFlashOn) {
                        R.string.imaging_content_description_flash_on
                    } else {
                        R.string.imaging_content_description_flash_off
                    }
                ),
                tint = MaterialTheme.colors.buttonText,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
            )
        }
    }
}
