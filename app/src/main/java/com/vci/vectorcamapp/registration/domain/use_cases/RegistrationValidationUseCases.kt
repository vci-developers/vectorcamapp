package com.vci.vectorcamapp.registration.domain.use_cases

import javax.inject.Inject

data class RegistrationValidationUseCases @Inject constructor(
    val validateCollectorTitle: ValidateCollectorTitleUseCase,
    val validateCollectorName: ValidateCollectorNameUseCase,
)
