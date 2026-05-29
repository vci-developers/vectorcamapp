package com.vci.vectorcamapp.intake.presentation

import com.vci.vectorcamapp.navigation.Destination

sealed interface IntakeEvent {
    data class NavigateAfterIntake(val destination: Destination) : IntakeEvent
    data object NavigateBackToRegistrationScreen : IntakeEvent
    data object NavigateBackToPreviousScreen : IntakeEvent
}
