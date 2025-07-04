package com.vci.vectorcamapp.incomplete_session.presentation

sealed interface IncompleteSessionEvent {
    data object NavigateToSurveillanceForm : IncompleteSessionEvent
    data object NavigateBack : IncompleteSessionEvent
}