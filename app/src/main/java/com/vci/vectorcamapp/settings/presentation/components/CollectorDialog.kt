package com.vci.vectorcamapp.settings.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.vci.vectorcamapp.core.presentation.components.button.TrackedActionButton
import com.vci.vectorcamapp.core.presentation.components.button.TrackedTextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.util.collector.CollectorValidationError
import com.vci.vectorcamapp.core.presentation.components.form.DatePickerField
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.settings.domain.model.SettingsDropdownOptions
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun CollectorDialog(
    collector: Collector,
    collectorNameError: CollectorValidationError?,
    collectorTitleError: CollectorValidationError?,
    collectorLastTrainedOnError: CollectorValidationError?,
    onNameChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onLastTrainedOnChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    isEditDialogVisible: Boolean,
    isDeleteDialogVisible: Boolean,
    modifier: Modifier = Modifier
) {

    AlertDialog(
        onDismissRequest = if (isDeleteDialogVisible) onDismissDeleteDialog else onDismiss,
        title = {
            Text(
                text = if (isDeleteDialogVisible) "Delete Profile?"
                else if (isEditDialogVisible) "Edit Profile"
                else "Add Profile"
            )
        },
        text = {
            if (isDeleteDialogVisible) {
                Text(
                    text = "This will permanently delete this collector profile. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.textSecondary
                )
            }
            else {
                Column {
                    TextEntryField(
                        label = "Collector Name",
                        value = collector.name,
                        onValueChange = onNameChange,
                        singleLine = true,
                        error = collectorNameError,
                    )

                    Spacer(modifier = Modifier.size(MaterialTheme.dimensions.spacingSmall))

                    DropdownField(
                        label = "Collector Title",
                        options = SettingsDropdownOptions.CollectorTitleOption.entries,
                        selectedOption = SettingsDropdownOptions.CollectorTitleOption.entries.firstOrNull { it.label == collector.title },
                        onOptionSelected = { option ->
                            onTitleChange(option.label)
                        },
                        error = collectorTitleError,
                        modifier = modifier
                            .fillMaxWidth()
                            .height(MaterialTheme.dimensions.componentHeightLarge)
                    ) { option ->
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.size(MaterialTheme.dimensions.spacingSmall))

                    DatePickerField(
                        label = "When were you last trained?",
                        selectedDateInMillis = collector.lastTrainedOn,
                        onDateSelected = onLastTrainedOnChange,
                        error = collectorLastTrainedOnError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TrackedActionButton(
                label = if (isDeleteDialogVisible) "Yes, Delete" else "Submit",
                onClick = if (isDeleteDialogVisible) onConfirmDelete else onSave,
                enabled = isDeleteDialogVisible || (collector.name.isNotBlank() && collector.title.isNotBlank()),
                textSize = MaterialTheme.typography.bodyMedium,
                feature = "CollectorDialog",
                action = if (isDeleteDialogVisible) "ConfirmDelete" else "Save"
            )
        },
        dismissButton = {
            if (isEditDialogVisible && !isDeleteDialogVisible) {
                TrackedTextButton(
                    label = "Delete",
                    onClick = onDelete,
                    feature = "CollectorDialog",
                    action = "Delete"
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colors.error,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                }
            } else {
                TrackedTextButton(
                    label = "Cancel",
                    onClick = if (isDeleteDialogVisible) onDismissDeleteDialog else onDismiss,
                    feature = "CollectorDialog",
                    action = if (isDeleteDialogVisible) "DismissDeleteDialog" else "Dismiss"
                ) {
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
