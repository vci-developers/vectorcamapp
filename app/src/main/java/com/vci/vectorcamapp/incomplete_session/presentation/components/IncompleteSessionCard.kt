package com.vci.vectorcamapp.incomplete_session.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.vci.vectorcamapp.core.domain.model.Session

@Composable
fun IncompleteSessionCard(session: Session) {
    Text(session.id.toString())
}
