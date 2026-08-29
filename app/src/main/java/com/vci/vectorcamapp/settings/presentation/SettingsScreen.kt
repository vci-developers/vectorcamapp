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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.core.presentation.util.locale.SupportedLanguage
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
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter =
        remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    ScreenHeader(
        title = stringResource(R.string.settings_title_screen),
        subtitle = stringResource(R.string.settings_body_subtitle),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.settings_content_description_back),
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable { onAction(SettingsAction.ReturnToLandingScreen) }
            )
        },
        modifier = modifier
    ) {
        item {
            SettingsSection(title = stringResource(R.string.settings_title_actions)) {
                SettingsActionTile(
                    title = stringResource(R.string.settings_title_start_data_collection),
                    onClick = { onAction(SettingsAction.StartNewDataCollectionSession) },
                    modifier = modifier
                )

                SettingsActionTile(
                    title = stringResource(R.string.settings_title_start_practice),
                    onClick = { onAction(SettingsAction.StartNewPracticeSession) },
                    modifier = modifier
                )
            }

            SettingsSection(title = stringResource(R.string.settings_title_preferences)) {
                SettingsInfoTile(title = stringResource(R.string.settings_title_language)) {
                    DropdownField(
                        options = SupportedLanguage.entries,
                        selectedOption = state.selectedLanguage,
                        onOptionSelected = { language ->
                            onAction(SettingsAction.SelectLanguage(language))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaterialTheme.dimensions.componentHeightLarge)
                    ) { language ->
                        Text(
                            text = stringResource(language.displayNameResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.settings_title_update_data)) {
                SettingsInfoTile(
                    title = stringResource(R.string.settings_title_cloud_sync),
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
                                text = if (state.isConnectedToInternet) stringResource(R.string.settings_body_connected) else stringResource(R.string.settings_body_no_internet),
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
                                    label = when {
                                        state.modelDownloadProgress != null -> "Downloading Model..."
                                        state.isSyncingData -> stringResource(R.string.settings_action_syncing)
                                        else -> stringResource(R.string.settings_action_resync)
                                    },
                                    onClick = {
                                        if (state.isConnectedToInternet && !state.isSyncingData) {
                                            onAction(SettingsAction.ResyncProgramData)
                                        }
                                    },
                                    textSize = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (state.isSyncingData && state.modelDownloadProgress == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(start = MaterialTheme.dimensions.paddingMedium)
                                        .size(MaterialTheme.dimensions.iconSizeMedium),
                                    color = MaterialTheme.colors.primary
                                )
                            }
                        }

                        val downloadProgress = state.modelDownloadProgress
                        if (downloadProgress != null) {
                            val percent = (downloadProgress * 100).roundToInt().coerceIn(0, 100)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colors.primary,
                                    trackColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                                )
                                Text(
                                    text = if (state.modelDownloadTotalBytes > 0L) {
                                        "Downloading ML model… $percent% " +
                                            "(${formatModelBytes(state.modelDownloadBytes)} / " +
                                            "${formatModelBytes(state.modelDownloadTotalBytes)})"
                                    } else {
                                        "Downloading ML model… $percent%"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colors.textSecondary
                                )
                            }
                        }

                        Text(
                            text = state.localModelIds?.let { ids ->
                                "ML models downloaded: $ids"
                            } ?: "ML model: using bundled assets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.textSecondary
                        )
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_title_about)) {
                SettingsInfoTile(
                    title = stringResource(R.string.settings_title_registered_collectors),
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
                                    contentDescription = stringResource(R.string.settings_content_description_profile),
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
                                        text = stringResource(R.string.settings_action_edit),
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
                        label = stringResource(R.string.settings_action_add_profile),
                        onClick = { onAction(SettingsAction.ShowAddCollectorDialog) },
                        textSize = MaterialTheme.typography.bodyMedium
                    )
                }
                SettingsInfoTile(
                    title = stringResource(R.string.settings_title_device_info),
                    modifier = modifier
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_label_device_id, state.device.id),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = stringResource(R.string.settings_label_device_model, state.device.model),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = stringResource(R.string.settings_label_registered_at, dateTimeFormatter.format(state.device.registeredAt)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                    }
                }
                SettingsInfoTile(
                    title = stringResource(R.string.settings_title_program_info),
                    modifier = modifier
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_label_program_id, state.program.id),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = stringResource(R.string.settings_label_program_name, state.program.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = stringResource(R.string.settings_label_country, state.program.country),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                    }
                }
                SettingsInfoTile(
                    title = stringResource(R.string.settings_title_app_info),
                    modifier = modifier
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_label_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = stringResource(R.string.settings_label_build_code, BuildConfig.VERSION_CODE),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textSecondary
                        )
                        Text(
                            text = stringResource(R.string.settings_label_build_type, BuildConfig.BUILD_TYPE),
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

private fun formatModelBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
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