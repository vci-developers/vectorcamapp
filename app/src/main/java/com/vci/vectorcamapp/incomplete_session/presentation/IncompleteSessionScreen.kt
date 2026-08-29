package com.vci.vectorcamapp.incomplete_session.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.search.SearchTextField
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.core.presentation.search.SearchHelpTooltipContent
import com.vci.vectorcamapp.incomplete_session.presentation.components.IncompleteSessionCard
import com.vci.vectorcamapp.incomplete_session.presentation.util.IncompleteSessionTestTags
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun IncompleteSessionScreen(
    state: IncompleteSessionState,
    onAction: (IncompleteSessionAction) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenHeader(
        title = stringResource(R.string.incomplete_session_title_screen),
        subtitle = stringResource(R.string.incomplete_session_body_subtitle),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.incomplete_session_content_description_back),
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable {
                        onAction(IncompleteSessionAction.ReturnToLandingScreen)
                    }
                    .testTag(IncompleteSessionTestTags.BACK_BUTTON) )
        },
        modifier = modifier
    ) {
        item {
            SearchTextField(
                searchQuery = state.searchQuery,
                onSearchQueryChange = { newSearchQueryText ->
                    onAction(IncompleteSessionAction.UpdateSearchQuery(newSearchQueryText))
                },
                placeholder = stringResource(R.string.incomplete_session_placeholder_search),
                modifier = Modifier.padding(
                    start = MaterialTheme.dimensions.spacingMedium,
                    end = MaterialTheme.dimensions.spacingMedium,
                    top = MaterialTheme.dimensions.spacingSmall
                ),
                isTooltipVisible = state.isSearchTooltipVisible,
                onShowSearchTooltip = { onAction(IncompleteSessionAction.ShowSearchTooltipDialog) },
                onDismissSearchTooltip = { onAction(IncompleteSessionAction.HideSearchTooltipDialog) }
            ) {
                SearchHelpTooltipContent()
            }
        }

        if (state.sessionAndSites.isEmpty()) {
            item {
                Text(
                    text = if (state.searchQuery.isBlank())
                        stringResource(R.string.incomplete_session_body_empty)
                    else
                        stringResource(R.string.incomplete_session_body_no_results),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium).fillMaxWidth()
                )
            }
        }

        itemsIndexed(
            items = state.sessionAndSites.asReversed(),
            key = { _, sessionAndSite -> sessionAndSite.session.localId }
        ) { index, sessionAndSite ->
            IncompleteSessionCard(
                sessionAndSite = sessionAndSite,
                onClick = { onAction(IncompleteSessionAction.ResumeSession(sessionAndSite.session.localId)) },
                onDelete = { onAction(IncompleteSessionAction.DeleteSession(sessionAndSite.session.localId)) },
                modifier = Modifier.testTag("${IncompleteSessionTestTags.CARD_PREFIX}-$index")
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
                    text = stringResource(R.string.incomplete_session_title_delete_dialog),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.incomplete_session_body_delete_dialog),
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
                        text = stringResource(R.string.incomplete_session_action_delete_dialog_confirm),
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
                        stringResource(R.string.incomplete_session_action_delete_dialog_cancel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                }
            }
        )
    }
}
