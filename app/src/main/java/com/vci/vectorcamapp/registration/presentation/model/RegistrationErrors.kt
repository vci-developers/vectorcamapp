package com.vci.vectorcamapp.registration.presentation.model

import com.vci.vectorcamapp.registration.domain.util.RegistrationValidationError

data class RegistrationErrors(
    val collectorName: RegistrationValidationError? = null,
    val collectorTitle: RegistrationValidationError? = null
)
