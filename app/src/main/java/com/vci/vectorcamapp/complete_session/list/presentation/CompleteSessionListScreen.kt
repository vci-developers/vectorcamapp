package com.vci.vectorcamapp.complete_session.list.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.complete_session.list.presentation.components.CompleteSessionListCard

@Composable
fun CompleteSessionListScreen(
    state: CompleteSessionListState,
    onAction: (CompleteSessionListAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = state.sessions.asReversed(), key = { it.id }) { session ->
            CompleteSessionListCard(
                session = session,
                modifier = Modifier
                    .clickable {
                        onAction(CompleteSessionListAction.ViewCompleteSessionDetail(session.id.toString()))
                    }
                    .then(modifier)
            )
        }
    }
}
