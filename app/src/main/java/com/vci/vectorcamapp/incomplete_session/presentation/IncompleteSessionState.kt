package com.vci.vectorcamapp.incomplete_session.presentation

import com.vci.vectorcamapp.core.domain.model.Session

data class IncompleteSessionState(
    val sessions: List<Session> = emptyList(),
)
