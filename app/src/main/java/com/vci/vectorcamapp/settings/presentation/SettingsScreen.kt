package com.vci.vectorcamapp.settings.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.settings.presentation.components.CollectorDialog
import com.vci.vectorcamapp.settings.presentation.components.CollectorWarningDialog
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
                    .size(MaterialTheme.dimensions.iconSizeLarge)
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

                SettingsActionTile(
                    title = "Start Practice Session",
                    onClick = { onAction(SettingsAction.StartNewPracticeSession) },
                    modifier = modifier
                )
            }

            SettingsSection(title = "Data Synchronization") {
                SettingsInfoTile(
                    title = "Cloud Sync",
                    modifier = modifier
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(MaterialTheme.dimensions.componentHeightExtraExtraSmall)
                                    .background(
                                        color = if (state.isConnectedToInternet) MaterialTheme.colors.successConfirm else MaterialTheme.colors.error,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (state.isConnectedToInternet) "Connected to Internet" else "No Internet Connection - Resync Unavailable",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.isConnectedToInternet) MaterialTheme.colors.textSecondary else MaterialTheme.colors.error
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(if (state.isConnectedToInternet) 1f else 0.5f)
                            ) {
                                ActionButton(
                                    label = if (state.isSyncingData) "Syncing Data..." else "Resync Data",
                                    onClick = {
                                        if (state.isConnectedToInternet && !state.isSyncingData) {
                                            onAction(SettingsAction.ResyncProgramData)
                                        }
                                    },
                                    textSize = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (state.isSyncingData) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(start = MaterialTheme.dimensions.paddingMedium)
                                        .size(MaterialTheme.dimensions.iconSizeMedium),
                                    color = MaterialTheme.colors.primary
                                )
                            }
                        }
                    }
                }
            }

            SettingsSection("About") {
                SettingsInfoTile(
                    title = "Registered Collectors",
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
                                        .size(MaterialTheme.dimensions.iconSizeLarge)
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
                                    thickness = MaterialTheme.dimensions.dividerThicknessThick
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

    state.selectedCollector?.let { collector ->
        CollectorDialog(
            collector = collector,
            collectorNameError = state.settingsErrors.collectorName,
            collectorTitleError = state.settingsErrors.collectorTitle,
            collectorLastTrainedOnError = state.settingsErrors.collectorLastTrainedOn,
            onNameChange = { onAction(SettingsAction.EnterCollectorName(it)) },
            onTitleChange = { onAction(SettingsAction.EnterCollectorTitle(it)) },
            onLastTrainedOnChange = { onAction(SettingsAction.EnterCollectorLastTrainedOn(it)) },
            onDismiss = { onAction(SettingsAction.DismissCollectorDialog) },
            onSave = { onAction(SettingsAction.SaveCollector) },
            onDelete = { onAction(SettingsAction.ShowDeleteCollectorDialog) },
            onConfirmDelete = { onAction(SettingsAction.ConfirmDeleteCollector) },
            onDismissDeleteDialog = { onAction(SettingsAction.DismissDeleteCollectorDialog) },
            isEditDialogVisible = state.isEditCollectorDialogVisible,
            isDeleteDialogVisible = state.isDeleteCollectorDialogVisible
        )
    }

    if (state.selectedCollector != null && state.similarCollector != null) {
        CollectorWarningDialog(
            selectedCollector = state.selectedCollector,
            similarCollector = state.similarCollector,
            onConfirmSave = { onAction(SettingsAction.ConfirmSaveCollector) },
            onDismiss = { onAction(SettingsAction.DismissCollectorWarningDialog) }
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