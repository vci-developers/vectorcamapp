package com.vci.vectorcamapp.complete_session.list.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.complete_session.list.presentation.components.CompleteSessionListTile
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
    Box(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Complete Sessions",
            subtitle = "Click on a session to view more details",
            modifier = modifier
        ) {
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
        val (buttonColor, icon, description) = if (!state.hasActiveUploads) {
            Triple(MaterialTheme.colors.primary, painterResource(id = R.drawable.ic_cloud_upload), "Upload All")
        } else {
            Triple(MaterialTheme.colors.warning, Icons.Default.Refresh, "Refresh")
        }

        FloatingActionButton(
            onClick = { onAction(CompleteSessionListAction.UploadAllPendingSessions) },
            containerColor = buttonColor,
            contentColor = MaterialTheme.colors.buttonText,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MaterialTheme.dimensions.paddingLarge)
        ) {
            if (!state.hasActiveUploads) {
                Icon(painter = icon as Painter, contentDescription = description)
            } else {
                Icon(icon as ImageVector, contentDescription = description)
            }
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
