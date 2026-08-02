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
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
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
                text = if (isDeleteDialogVisible) stringResource(R.string.settings_title_delete_profile)
                else if (isEditDialogVisible) stringResource(R.string.settings_title_edit_profile)
                else stringResource(R.string.settings_title_add_profile)
            )
        },
        text = {
            if (isDeleteDialogVisible) {
                Text(
                    text = stringResource(R.string.settings_body_delete_collector),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.textSecondary
                )
            }
            else {
                Column {
                    TextEntryField(
                        label = stringResource(R.string.settings_label_collector_name),
                        value = collector.name,
                        onValueChange = onNameChange,
                        singleLine = true,
                        error = collectorNameError,
                    )

                    Spacer(modifier = Modifier.size(MaterialTheme.dimensions.spacingSmall))

                    DropdownField(
                        label = stringResource(R.string.settings_label_collector_title),
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
                        label = stringResource(R.string.settings_label_last_trained),
                        selectedDateInMillis = collector.lastTrainedOn,
                        onDateSelected = onLastTrainedOnChange,
                        error = collectorLastTrainedOnError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = if (isDeleteDialogVisible) onConfirmDelete else onSave,
                enabled = isDeleteDialogVisible || (collector.name.isNotBlank() && collector.title.isNotBlank()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDeleteDialogVisible) MaterialTheme.colors.error else MaterialTheme.colors.secondary,
                    contentColor = MaterialTheme.colors.buttonText
                )
            ) {
                Text(text = if (isDeleteDialogVisible) stringResource(R.string.settings_action_collector_dialog_confirm_delete) else stringResource(R.string.settings_action_collector_dialog_submit), style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = {
            if (isEditDialogVisible && !isDeleteDialogVisible) {
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.settings_action_collector_dialog_delete),
                        color = MaterialTheme.colors.error,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                }
            } else {
                TextButton(onClick = if (isDeleteDialogVisible) onDismissDeleteDialog else onDismiss) {
                    Text(
                        text = stringResource(R.string.settings_action_collector_dialog_cancel),
                        color = MaterialTheme.colors.textSecondary,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                }
            }
        },
        containerColor = MaterialTheme.colors.cardBackground
    )
}
