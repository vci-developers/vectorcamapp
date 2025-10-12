package com.vci.vectorcamapp.main.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.tooltip.Tooltip
import com.vci.vectorcamapp.main.presentation.components.PermissionTooltipRow
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun PermissionScreen(
    state: MainState, onAction: (MainAction) -> Unit, modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (!state.allGranted) {
            onAction(MainAction.RequestPermissions)
        }
    }

    Column(modifier=Modifier.fillMaxHeight().background(MaterialTheme.colors.cardBackground), verticalArrangement = Arrangement.Center) {
        Column(
            modifier = modifier
                .padding(
                    horizontal = MaterialTheme.dimensions.paddingExtraExtraLarge,
                    vertical = MaterialTheme.dimensions.paddingExtraExtraLarge
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.permission_background),
                contentDescription = "Mosquito background",
                contentScale = ContentScale.Fit,
                modifier = modifier
                    .padding(horizontal = MaterialTheme.dimensions.paddingLarge)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingLarge))

            Text(
                text = if (!state.allGranted) "Permissions Required" else "GPS Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

            Text(
                text = "To work properly, this app needs access to your camera, location, and notifications. One or more of these permissions are currently disabled. Please enable them in settings to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

            Tooltip(
                isVisible = state.isPermissionTooltipVisible,
                onClick = { onAction(MainAction.ShowPermissionTooltipDialog) },
                onDismiss = { onAction(MainAction.HidePermissionTooltipDialog) },
                buttonText = "Tap to learn more about app permissions"
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                ) {
                    Text(
                        text = "App Permissions",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = MaterialTheme.dimensions.paddingSmall)
                    )
                    PermissionTooltipRow(
                        title = "Camera Permission",
                        description = "Required to capture detailed images of mosquito specimens for accurate identification and analysis.",
                        iconPainter = painterResource(id = R.drawable.ic_camera),
                        iconDescription = "Camera",
                    )
                    PermissionTooltipRow(
                        title = "Location Permission",
                        description = "Needed to log exact collection sites and associate each specimen with its geographic origin for field tracking.",
                        iconPainter = painterResource(id = R.drawable.ic_pin),
                        iconDescription = "Location"
                    )
                    PermissionTooltipRow(
                        title = "Notification Permission",
                        description = "Allows the app to show upload progress, completion alerts, and other key background status updates.",
                        iconPainter = painterResource(id = R.drawable.ic_notification),
                        iconDescription = "Notification"
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingExtraLarge))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
            ) {
                if (!state.allGranted) {
                    ActionButton(
                        onClick = { onAction(MainAction.OpenAppSettings) },
                        label = "Grant Permissions"
                    )
                }
                if (!state.isGpsEnabled) {
                    ActionButton(
                        onClick = { onAction(MainAction.OpenLocationSettings) },
                        label = "Enable GPS"
                    )
                }
            }
        }
    }
}
