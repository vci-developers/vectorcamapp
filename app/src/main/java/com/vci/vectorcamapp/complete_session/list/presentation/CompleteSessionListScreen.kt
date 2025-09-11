package com.vci.vectorcamapp.complete_session.list.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListAction.UpdateSearchQuery
import com.vci.vectorcamapp.complete_session.list.presentation.components.CompleteSessionListTile
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun CompleteSessionListScreen(
    state: CompleteSessionListState,
    onAction: (CompleteSessionListAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Complete Sessions",
            subtitle = "Click on a session to view more details",
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back Button",
                    tint = MaterialTheme.colors.icon,
                    modifier = Modifier
                        .size(MaterialTheme.dimensions.iconSizeMedium)
                        .clickable {
                            onAction(CompleteSessionListAction.ReturnToLandingScreen)
                        })
            },
            modifier = modifier
        ) {
            item {
                TextEntryField(
                    value = state.searchQuery,
                    onValueChange = { newSearchQuery -> onAction(UpdateSearchQuery(newSearchQuery)) },
                    placeholder = "Search by collector, district, session type, etc.",
                    modifier = Modifier.padding(
                        top = MaterialTheme.dimensions.paddingSmall,
                        start = MaterialTheme.dimensions.paddingMedium,
                        end = MaterialTheme.dimensions.paddingMedium),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    )
                )
            }

            if (state.sessionsAndSites.isEmpty()) {
                item {
                    Text(
                        text = if (state.searchQuery.isBlank())
                            "No completed sessions found."
                        else
                            "No matching sessions found.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(MaterialTheme.dimensions.paddingMedium)
                            .fillMaxSize()
                    )
                }
            }

            items(
                items = state.sessionsAndSites.asReversed(),
                key = { it.session.localId }) { sessionAndSite ->
                CompleteSessionListTile(
                    session = sessionAndSite.session, site = sessionAndSite.site, onClick = {
                        onAction(
                            CompleteSessionListAction.ViewCompleteSessionDetails(
                                sessionAndSite.session.localId
                            )
                        )
                    })
            }
        }

        FloatingActionButton(
            onClick = { onAction(CompleteSessionListAction.UploadAllPendingSessions) },
            containerColor = if (state.isUploading) MaterialTheme.colors.warning else MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.buttonText,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MaterialTheme.dimensions.paddingLarge)
        ) {
            Icon(
                painter = if (state.isUploading) painterResource(id = R.drawable.ic_refresh) else painterResource(
                    id = R.drawable.ic_cloud_upload
                ),
                contentDescription = if (state.isUploading) "Refresh" else "Upload",
                tint = MaterialTheme.colors.buttonText,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun CompleteSessionListScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            CompleteSessionListScreen(
                state = CompleteSessionListState(),
                onAction = {},
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
