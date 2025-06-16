package com.vci.vectorcamapp.complete_session.list.presentation

sealed class CompleteSessionListAction {
    data class ViewCompleteSessionDetails(val sessionId: String) : CompleteSessionListAction()
    data class ViewCompleteSessionSpecimens(val sessionId: String) : CompleteSessionListAction()
}
