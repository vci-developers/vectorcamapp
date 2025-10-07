package com.vci.vectorcamapp.incomplete_session.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.incomplete_session.presentation.components.IncompleteSessionCard
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun IncompleteSessionScreen(
    state: IncompleteSessionState,
    onAction: (IncompleteSessionAction) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenHeader(
        title = "Incomplete Sessions",
        subtitle = "Click on a session to resume",
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Back Button",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable {
                        onAction(IncompleteSessionAction.ReturnToLandingScreen)
                    })
        },
        modifier = modifier
    ) {
        items(
            items = state.sessions.asReversed(),
            key = { it.localId }
        ) { session ->
            IncompleteSessionCard(
                session = session,
                onClick = { onAction(IncompleteSessionAction.ResumeSession(session.localId)) },
                onDelete = { onAction(IncompleteSessionAction.DeleteSession(session.localId)) }
            )
        }
    }

    if (state.deleteDialogSessionId != null) {
        AlertDialog(
            onDismissRequest = {
                onAction(IncompleteSessionAction.DismissDeleteDialog)
            },
            title = {
                Text(
                    text = "Delete Session?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently delete the session and all associated images from your device. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAction(IncompleteSessionAction.ConfirmDeleteSession(state.deleteDialogSessionId))
                    },
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
                    onClick = { onAction(IncompleteSessionAction.DismissDeleteDialog) }
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }
            }
        )
    }
}
