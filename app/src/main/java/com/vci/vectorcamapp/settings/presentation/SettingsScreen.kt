package com.vci.vectorcamapp.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
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
            SettingsSection(title = "Actions",) {
                SettingsActionTile(
                    title = "Start Data Collection",
                    onClick = { onAction(SettingsAction.StartNewDataCollectionSession) },
                    modifier = modifier
                )
            }
            SettingsSection("System Information") {
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
