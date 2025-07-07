package com.vci.vectorcamapp.complete_session.details.presentation.components.specimens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Specimen

@Composable
fun CompleteSessionSpecimensScreen(
    session: Session,
    specimens: List<Specimen>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = specimens.asReversed(), key = { it.id }) { specimen ->
            CompleteSessionSpecimensCard(session = session, specimen = specimen, modifier = modifier)
        }
    }
}
