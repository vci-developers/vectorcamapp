package com.vci.vectorcamapp.incomplete_session.presentation

import com.vci.vectorcamapp.core.domain.model.Session
import java.util.UUID

data class IncompleteSessionState(
    val sessions: List<Session> = emptyList(),
    val deleteDialogSessionId: UUID? = null,
    val searchQuery: String = ""
)
