package com.vci.vectorcamapp.incomplete_session.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.search.SearchTextField
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
        item {
            SearchTextField(
                searchQuery = state.searchQuery,
                onSearchQueryChange = { newSearchQueryText ->
                    onAction(IncompleteSessionAction.UpdateSearchQuery(newSearchQueryText))
                },
                placeholder = "Search by collector, district, session type, etc.",
                modifier = Modifier.padding(
                    start = MaterialTheme.dimensions.paddingMedium,
                    end = MaterialTheme.dimensions.paddingMedium
                )
            )
        }

        if (state.sessionAndSites.isEmpty()) {
            item {
                Text(
                    text = if (state.searchQuery.isBlank())
                        "No incomplete sessions found."
                    else
                        "No matching sessions found.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium).fillMaxWidth()
                )
            }
        }

        items(
            items = state.sessionAndSites.asReversed(),
            key = { it.session.localId }
        ) { sessionAndSite ->
            IncompleteSessionCard(
                sessionAndSite = sessionAndSite,
                onClick = { onAction(IncompleteSessionAction.ResumeSession(sessionAndSite.session.localId)) },
                onDelete = { onAction(IncompleteSessionAction.DeleteSession(sessionAndSite.session.localId)) }
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
