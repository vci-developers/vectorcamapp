package com.vci.vectorcamapp.incomplete_session.presentation

sealed interface IncompleteSessionEvent {
    object NavigateToSurveillanceForm : IncompleteSessionEvent
}