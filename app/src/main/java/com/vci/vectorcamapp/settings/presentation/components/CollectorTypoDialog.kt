package com.vci.vectorcamapp.settings.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.ui.extensions.colors

@Composable
fun CollectorTypoDialog(
    collectorName: String,
    similarCollectorName: String,
    onConfirmSave: () -> Unit,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onEditName,
        title = {
            Text(
                text = "Possible Typo Detected",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "The name '$collectorName' is very similar to an existing collector '$similarCollectorName'. Are you sure you want to add this profile?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colors.secondary,
                    contentColor = MaterialTheme.colors.buttonText
                )
            ) {
                Text(
                    text = "Save Anyway",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onEditName) {
                Text(
                    text = "Edit Name",
                    color = MaterialTheme.colors.textSecondary,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
            }
        },
        containerColor = MaterialTheme.colors.cardBackground,
        modifier = modifier
    )
}
