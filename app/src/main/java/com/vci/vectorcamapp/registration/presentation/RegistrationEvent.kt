package com.vci.vectorcamapp.registration.presentation

sealed interface RegistrationEvent {
    object NavigateToLanding : RegistrationEvent
}