package com.vci.vectorcamapp.complete_session.list.presentation

import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.complete_session.list.presentation.components.CompleteSessionListTile
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState

@Composable
fun CompleteSessionListScreen(
    state: CompleteSessionListState,
    onAction: (CompleteSessionListAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val ROTATION_DURATION = 2000

    val hasActiveUploads = state.sessionAndSiteToUploadProgress.values.any { it.isUploading }

    val rotation by animateFloatAsState(
        targetValue = if (hasActiveUploads) 360f else 0f,
        animationSpec = if (hasActiveUploads) {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = ROTATION_DURATION,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(0)
        },
        label = "rotation"
    )

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
            items(
                items = state.sessionAndSiteToUploadProgress.toList().asReversed(),
                key = { it.first.session.localId }) { (sessionAndSite, sessionUploadProgress) ->

                CompleteSessionListTile(
                    sessionAndSite = sessionAndSite,
                    sessionUploadProgress = sessionUploadProgress,
                    onClick = {
                        onAction(
                            CompleteSessionListAction.ViewCompleteSessionDetails(
                                sessionAndSite.session.localId
                            )
                        )
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { onAction(CompleteSessionListAction.UploadAllPendingSessions) },
            containerColor = if (hasActiveUploads) MaterialTheme.colors.warning else MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.buttonText,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MaterialTheme.dimensions.paddingLarge)
        ) {
            Icon(
                painter = if (hasActiveUploads) painterResource(id = R.drawable.ic_refresh) else painterResource(
                    id = R.drawable.ic_cloud_upload
                ),
                contentDescription = if (hasActiveUploads) "Refresh" else "Upload",
                tint = MaterialTheme.colors.buttonText,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeMedium)
                    .rotate(rotation)
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
