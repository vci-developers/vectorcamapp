package com.vci.vectorcamapp.complete_session.list.presentation

sealed interface CompleteSessionListEvent {
    data class NavigateToCompleteSessionDetails(val sessionId: String) : CompleteSessionListEvent
}
