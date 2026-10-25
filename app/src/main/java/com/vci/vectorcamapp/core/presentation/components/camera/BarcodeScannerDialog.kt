package com.vci.vectorcamapp.core.presentation.components.camera

import androidx.camera.core.SurfaceRequest
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun BarcodeScannerDialog(
    title: String,
    description: String,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    qrOnly: Boolean = false,
    transformValue: (String) -> String? = { it.takeIf(String::isNotBlank) },
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onCameraReleased: () -> Unit = {},
) {
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    BindBarcodeScanner(
        onBarcodeScanned = onBarcodeScanned,
        onSurfaceRequest = { request -> surfaceRequest = request },
        qrOnly = qrOnly,
        transformValue = transformValue,
        cameraSelector = cameraSelector,
        onCameraReleased = onCameraReleased,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .padding(MaterialTheme.dimensions.paddingMedium)
                .fillMaxWidth(),
            shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
            color = MaterialTheme.colors.cardBackground,
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.core_content_description_close_scanner),
                            tint = MaterialTheme.colors.icon,
                        )
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.textSecondary,
                )

                CameraPreview(
                    surfaceRequest = surfaceRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f / MaterialTheme.dimensions.aspectRatio)
                        .clip(RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall))
                        .background(Color.Black),
                    overlay = {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val inset = MaterialTheme.dimensions.paddingLarge
                            val side = minOf(maxWidth, maxHeight) - (inset * 2)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(side)
                                    .border(
                                        width = MaterialTheme.dimensions.borderThicknessThick,
                                        color = MaterialTheme.colors.buttonText,
                                        shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall),
                                    )
                            )
                        }
                    },
                )
            }
        }
    }
}
