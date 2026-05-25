package com.vci.vectorcamapp.hour_log.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.hour_log.presentation.components.HourSessionCard
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun HourLogScreen(
    state: HourLogState,
    onAction: (HourLogAction) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenHeader(
        title = "Hour Log",
        subtitle = "Click on session to resume",
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Add Hour",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable { onAction(HourLogAction.NavigateToAddHour) }
            )
        },
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_cloud_upload),
                contentDescription = "Upload",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable { onAction(HourLogAction.UploadSession) }
            )
        },
        modifier = modifier
    ) {
        if (state.hourSessions.isEmpty()) {
            item {
                Text(
                    text = "No hour sessions yet.\nTap + to add one.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.dimensions.paddingMedium)
                )
            }
        }

        items(
            items = state.hourSessions,
            key = { it.id }
        ) { hourSession ->
            HourSessionCard(
                hourSession = hourSession,
                onClick = { onAction(HourLogAction.ResumeHourSession(hourSession.id)) }
            )
        }
    }
}
