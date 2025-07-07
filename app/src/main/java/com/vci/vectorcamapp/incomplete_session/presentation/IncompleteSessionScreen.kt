package com.vci.vectorcamapp.incomplete_session.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.core.presentation.components.ui.ScreenHeader
import com.vci.vectorcamapp.incomplete_session.presentation.components.IncompleteSessionCard
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
        modifier = modifier
    ) {
        item { Spacer(Modifier.height(MaterialTheme.dimensions.spacingSmall)) }
        items(
            items = state.sessions.asReversed(),
            key = { it.localId }
        ) { session ->
            IncompleteSessionCard(
                session = session,
                onClick = { onAction(IncompleteSessionAction.ResumeSession(session.localId)) }
            )
            Spacer(Modifier.height(MaterialTheme.dimensions.spacingSmall))
        }
    }
}
