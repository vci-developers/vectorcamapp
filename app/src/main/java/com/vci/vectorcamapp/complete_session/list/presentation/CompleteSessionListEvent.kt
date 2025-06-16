package com.vci.vectorcamapp.complete_session.list.presentation

sealed class CompleteSessionListEvent {
    data class NavigateToCompleteSessionDetails(val sessionId: String) : CompleteSessionListEvent()
    data class NavigateToCompleteSessionSpecimens(val sessionId: String) : CompleteSessionListEvent()
}
