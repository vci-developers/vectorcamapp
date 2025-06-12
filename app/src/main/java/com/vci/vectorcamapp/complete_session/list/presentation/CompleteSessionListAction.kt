package com.vci.vectorcamapp.complete_session.list.presentation

sealed class CompleteSessionListAction {
    data class ViewCompleteSessionDetail(val sessionId: String) : CompleteSessionListAction()
}
