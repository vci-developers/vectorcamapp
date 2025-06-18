package com.vci.vectorcamapp.registration.presentation

sealed interface RegistrationAction {
    data class RegisterProgram(val programId: Int) : RegistrationAction
}