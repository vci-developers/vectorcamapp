package com.vci.vectorcamapp.complete_session.presentation

import com.vci.vectorcamapp.core.domain.model.Session

data class CompleteSessionState (
    val sessions: List<Session> = emptyList(),
)