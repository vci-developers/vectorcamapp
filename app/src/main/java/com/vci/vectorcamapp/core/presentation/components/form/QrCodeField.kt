package com.vci.vectorcamapp.core.presentation.components.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.components.camera.BarcodeScannerDialog
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun QrCodeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: Error? = null,
) {
    var scannerVisible by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextEntryField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            required = required,
            error = error,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { scannerVisible = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = stringResource(R.string.core_title_scan_qr_code),
                tint = MaterialTheme.colors.icon,
            )
        }
    }

    if (scannerVisible) {
        BarcodeScannerDialog(
            title = stringResource(R.string.core_title_scan_qr_code),
            description = stringResource(R.string.core_body_scan_qr_code),
            qrOnly = true,
            onBarcodeScanned = {
                onValueChange(it)
                scannerVisible = false
            },
            onDismiss = { scannerVisible = false },
        )
    }
}
