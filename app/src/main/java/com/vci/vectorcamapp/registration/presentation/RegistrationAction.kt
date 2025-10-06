package com.vci.vectorcamapp.registration.presentation

import com.vci.vectorcamapp.core.domain.model.Program

sealed interface RegistrationAction {
    data class SelectProgram(val program: Program) : RegistrationAction
    data class EnterCollectorName(val text: String) : RegistrationAction
    data class EnterCollectorTitle(val text: String) : RegistrationAction
    data object ConfirmRegistration : RegistrationAction
}
