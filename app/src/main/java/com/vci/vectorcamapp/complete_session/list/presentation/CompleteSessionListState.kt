package com.vci.vectorcamapp.complete_session.list.presentation

import com.vci.vectorcamapp.core.domain.model.Session

data class CompleteSessionListState (
    val sessions: List<Session> = emptyList(),
)
