package com.vci.vectorcamapp.incomplete_session.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.incomplete_session.presentation.components.IncompleteSessionCard
import com.vci.vectorcamapp.incomplete_session.presentation.components.PageHeader
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.screenHeightFraction

@Composable
fun IncompleteSessionScreen(
    state: IncompleteSessionState,
    onAction: (IncompleteSessionAction) -> Unit
) {
    val pageHeaderHeight = 0.25f
    val pageBodyOffset = 0.2f

    Box(Modifier.fillMaxSize()) {

        PageHeader(
            title     = "Incomplete Sessions",
            subtitle  = "Click on session to resume",
            onBack    = { onAction(IncompleteSessionAction.ReturnToMain) },
            modifier  = Modifier
                .fillMaxWidth()
                .height(screenHeightFraction(pageHeaderHeight))
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = screenHeightFraction(pageBodyOffset))
                .padding(horizontal = MaterialTheme.dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
        ) {
            items(
                items = state.sessions.asReversed(),
                key   = { it.localId }
            ) { session ->
                IncompleteSessionCard(
                    session  = session,
                    onClick  = { onAction(IncompleteSessionAction.ResumeSession(session.localId)) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(screenHeightFraction(pageBodyOffset)))
            }
        }
    }
}