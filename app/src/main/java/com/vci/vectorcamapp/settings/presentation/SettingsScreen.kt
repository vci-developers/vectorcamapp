package com.vci.vectorcamapp.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.settings.presentation.components.CollectorDialog
import com.vci.vectorcamapp.settings.presentation.components.SettingsActionTile
import com.vci.vectorcamapp.settings.presentation.components.SettingsInfoTile
import com.vci.vectorcamapp.settings.presentation.components.SettingsSection
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter =
        remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    ScreenHeader(
        title = "Settings",
        subtitle = "Configure app preferences",
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Back Button",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeMedium)
                    .clickable { onAction(SettingsAction.ReturnToLandingScreen) }
            )
        },
        modifier = modifier
    ) {
        item {
            SettingsSection(title = "Actions") {
                SettingsActionTile(
                    title = "Start Data Collection",
                    onClick = { onAction(SettingsAction.StartNewDataCollectionSession) },
                    modifier = modifier
                )
            }
            SettingsSection("About") {
                SettingsInfoTile(
                    title = "Collector Profiles",
                    modifier = modifier
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        state.collectors.forEachIndexed { index, collector ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAction(
                                            SettingsAction.ShowEditCollectorDialog(
                                                collector
                                            )
                                        )
                                    }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_person),
                                    contentDescription = "Profile Icon",
                                    tint = MaterialTheme.colors.icon,
                                    modifier = Modifier
                                        .padding(horizontal = MaterialTheme.dimensions.paddingSmall)
                                        .size(MaterialTheme.dimensions.iconSizeMedium)
                                )
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = MaterialTheme.dimensions.paddingSmall)
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = collector.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = collector.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colors.textSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(end = MaterialTheme.dimensions.paddingSmall),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Edit",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colors.icon,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (index < state.collectors.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colors.divider,
                                    thickness = MaterialTheme.dimensions.dividerThickness
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
                    ActionButton(
                        label = "Add Profile",
                        onClick = { onAction(SettingsAction.ShowAddCollectorDialog) },
                        textSize = MaterialTheme.typography.bodyMedium
                    )
                }
                SettingsInfoTile(
                    title = "Device Information",
                    modifier = modifier
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        Text(
                            text = "Device ID: ${state.device.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = "Device Model: ${state.device.model}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = "Registered At: ${dateTimeFormatter.format(state.device.registeredAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                    }
                }
                SettingsInfoTile(
                    title = "Program Information",
                    modifier = modifier
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        Text(
                            text = "Program ID: ${state.program.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = "Program Name: ${state.program.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = "Country: ${state.program.country}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                    }
                }
                SettingsInfoTile(
                    title = "App Information",
                    modifier = modifier
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        Text(
                            text = "Version: ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = "Build Code: ${BuildConfig.VERSION_CODE}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = "Build Type: ${BuildConfig.BUILD_TYPE}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }

    state.dialogCollector?.let { collector ->
        CollectorDialog(
            collector = collector,
            isEditMode = state.isCollectorDialogEditMode,
            nameError = state.settingsErrors.collectorName,
            titleError = state.settingsErrors.collectorTitle,
            onNameChange = { onAction(SettingsAction.EnterCollectorName(it)) },
            onTitleChange = { onAction(SettingsAction.EnterCollectorTitle(it)) },
            onDismiss = { onAction(SettingsAction.DismissCollectorDialog) },
            onSave = { onAction(SettingsAction.SaveCollector) },
            onDelete = if (state.isCollectorDialogEditMode) {
                { onAction(SettingsAction.ShowDeleteProfileDialog) }
            } else null
        )
    }

    if (state.isDeleteProfileDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(SettingsAction.DismissDeleteProfileDialog) },
            title = {
                Text(
                    text = "Delete Profile?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently delete this collector profile. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { onAction(SettingsAction.ConfirmDeleteCollector) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colors.error
                    )
                ) {
                    Text(
                        text = "Yes, Delete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.buttonText
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(SettingsAction.DismissDeleteProfileDialog) }
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }
            },
            containerColor = MaterialTheme.colors.cardBackground
        )
    }
}

@PreviewLightDark
@Composable
fun SettingsScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SettingsScreen(
                onAction = {},
                state = SettingsState(),
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}