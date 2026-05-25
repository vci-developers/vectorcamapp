package com.vci.vectorcamapp.intake.presentation

sealed interface IntakeEvent {
    data object NavigateToImagingScreen: IntakeEvent
    data class NavigateToHourLogScreen(val sessionId: String) : IntakeEvent
    data object NavigateBackToRegistrationScreen : IntakeEvent
    data object NavigateBackToPreviousScreen : IntakeEvent
}
