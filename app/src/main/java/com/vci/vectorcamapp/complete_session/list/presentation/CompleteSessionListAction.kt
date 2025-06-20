package com.vci.vectorcamapp.complete_session.list.presentation

sealed interface CompleteSessionListAction {
    data class ViewCompleteSessionDetails(val sessionId: String) : CompleteSessionListAction
}
