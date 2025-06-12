package com.vci.vectorcamapp.complete_session.list.presentation

sealed class CompleteSessionListEvent {
    data class NavigateToCompleteSessionDetail(val sessionId: String) : CompleteSessionListEvent()
}
