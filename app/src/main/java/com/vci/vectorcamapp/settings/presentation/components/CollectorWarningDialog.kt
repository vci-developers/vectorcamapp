package com.vci.vectorcamapp.settings.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.ui.extensions.colors
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CollectorWarningDialog(
    selectedCollector: Collector,
    similarCollector: Collector,
    onConfirmSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val formattedDate = dateFormatter.format(selectedCollector.lastTrainedOn)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Possible Typo Detected",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "The name '${selectedCollector.name}' is very similar to an existing collector '${similarCollector.name}'.\n\nTitle: ${selectedCollector.title}\nLast Trained On: $formattedDate\n\nAre you sure you want to add this profile?",                style = MaterialTheme.typography.bodyMedium,
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
            TextButton(onClick = onDismiss) {
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
