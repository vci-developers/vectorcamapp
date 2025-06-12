package com.vci.vectorcamapp.complete_session.detail.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.complete_session.detail.presentation.components.CompleteSessionDetailCard

@Composable
fun CompleteSessionDetailScreen(
    state: CompleteSessionDetailState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompleteSessionDetailCard(
            session = state.session,
            surveillanceForm = state.surveillanceForm,
            modifier = modifier
        )
    }
}
