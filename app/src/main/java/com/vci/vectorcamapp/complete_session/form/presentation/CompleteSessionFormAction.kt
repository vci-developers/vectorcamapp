package com.vci.vectorcamapp.complete_session.form.presentation

sealed interface CompleteSessionFormAction {
    data class LoadSession(val sessionId: String) : CompleteSessionFormAction
}
