package com.vci.vectorcamapp.settings.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.settings.domain.model.SettingsDropdownOptions
import com.vci.vectorcamapp.settings.domain.util.SettingsValidationError
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun CollectorDialog(
    collector: Collector,
    isEditMode: Boolean,
    nameError: SettingsValidationError?,
    titleError: SettingsValidationError?,
    onNameChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isEditMode) "Edit Profile" else "Add Profile")
        },
        text = {
            Column {
                TextEntryField(
                    label = "Collector Name",
                    value = collector.name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    error = nameError,
                )

                Spacer(modifier = Modifier.size(MaterialTheme.dimensions.spacingSmall))

                DropdownField(
                    label = "Collector Title",
                    options = SettingsDropdownOptions.CollectorTitleOption.entries,
                    selectedOption = SettingsDropdownOptions.CollectorTitleOption.entries.firstOrNull { it.label == collector.title },
                    onOptionSelected = { option ->
                        onTitleChange(option.label)
                    },
                    error = titleError,
                    modifier = modifier.fillMaxWidth().height(MaterialTheme.dimensions.componentHeightLarge)
                ) { option ->
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colors.secondary,
                    contentColor = MaterialTheme.colors.buttonText
                )
            ) {
                Text(text = "Submit", style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = {
            if (isEditMode && onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colors.error,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colors.textSecondary,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                }
            }
        },
        containerColor = MaterialTheme.colors.cardBackground
    )
}
