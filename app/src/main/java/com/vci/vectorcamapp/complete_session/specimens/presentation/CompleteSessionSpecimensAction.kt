package com.vci.vectorcamapp.complete_session.specimens.presentation

sealed interface CompleteSessionSpecimensAction {
    data class LoadSession(val sessionId: String) : CompleteSessionSpecimensAction
}
