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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.tooltip.Tooltip
import com.vci.vectorcamapp.main.presentation.components.PermissionTooltipRow
import com.vci.vectorcamapp.main.presentation.util.PermissionTestTags
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun PermissionScreen(
    state: MainState,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (!state.allGranted) {
            onAction(MainAction.RequestPermissions)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colors.cardBackground)
            .testTag(PermissionTestTags.ROOT),
        verticalArrangement = Arrangement.Center
    ) {
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
                contentDescription = stringResource(R.string.permission_content_description_background),
                contentScale = ContentScale.Fit,
                modifier = modifier
                    .padding(horizontal = MaterialTheme.dimensions.paddingLarge)
                    .fillMaxWidth()
                    .testTag(PermissionTestTags.IMAGE)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingLarge))

            Text(
                text = when {
                    !state.allGranted -> stringResource(R.string.permission_title_permissions_required)
                    !state.isGpsEnabled -> stringResource(R.string.permission_title_gps_required)
                    !state.isAutoTimeEnabled -> stringResource(R.string.permission_title_auto_time_required)
                    else -> stringResource(R.string.permission_title_permissions_required)
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(PermissionTestTags.TITLE)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

            Text(
                text = when {
                    !state.allGranted -> stringResource(R.string.permission_body_required)
                    !state.isGpsEnabled -> stringResource(R.string.permission_body_gps_required)
                    !state.isAutoTimeEnabled -> stringResource(R.string.permission_body_auto_time_required)
                    else -> stringResource(R.string.permission_body_required)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colors.textSecondary,
                textAlign = TextAlign.Left,
                lineHeight = MaterialTheme.typography.headlineLarge.lineHeight,
                modifier = Modifier
                    .padding(
                        start = MaterialTheme.dimensions.paddingMedium,
                        end = MaterialTheme.dimensions.paddingExtraSmall
                    )
                    .testTag(PermissionTestTags.DESCRIPTION)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))

            Tooltip(
                isVisible = state.isPermissionTooltipVisible,
                onClick = { onAction(MainAction.ShowPermissionTooltipDialog) },
                onDismiss = { onAction(MainAction.HidePermissionTooltipDialog) },
                buttonText = stringResource(R.string.permission_action_learn_more)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                    modifier = Modifier.testTag(PermissionTestTags.PERMISSION_TOOLTIP_CONTENT)
                ) {
                    Text(
                        text = stringResource(R.string.permission_title_tooltip),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = MaterialTheme.dimensions.paddingSmall)
                    )
                    PermissionTooltipRow(
                        title = stringResource(R.string.permission_title_camera),
                        description = stringResource(R.string.permission_body_camera),
                        iconPainter = painterResource(id = R.drawable.ic_camera),
                        iconDescription = stringResource(R.string.permission_content_description_camera),
                    )
                    PermissionTooltipRow(
                        title = stringResource(R.string.permission_title_location),
                        description = stringResource(R.string.permission_body_location),
                        iconPainter = painterResource(id = R.drawable.ic_pin),
                        iconDescription = stringResource(R.string.permission_content_description_location)
                    )
                    PermissionTooltipRow(
                        title = stringResource(R.string.permission_title_notification),
                        description = stringResource(R.string.permission_body_notification),
                        iconPainter = painterResource(id = R.drawable.ic_notification),
                        iconDescription = stringResource(R.string.permission_content_description_notification)
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
                        label = stringResource(R.string.permission_action_grant),
                        modifier = Modifier.testTag(PermissionTestTags.GRANT_PERMISSIONS_BUTTON)
                    )
                }
                if (!state.isGpsEnabled) {
                    ActionButton(
                        onClick = { onAction(MainAction.OpenLocationSettings) },
                        label = stringResource(R.string.permission_action_enable_gps),
                        modifier = Modifier.testTag(PermissionTestTags.ENABLE_GPS_BUTTON)
                    )
                }
                if (!state.isAutoTimeEnabled) {
                    ActionButton(
                        onClick = { onAction(MainAction.OpenDateSettings) },
                        label = stringResource(R.string.permission_action_enable_auto_time),
                        modifier = Modifier.testTag(PermissionTestTags.ENABLE_AUTO_TIME_BUTTON)
                    )
                }
            }
        }
    }
}
