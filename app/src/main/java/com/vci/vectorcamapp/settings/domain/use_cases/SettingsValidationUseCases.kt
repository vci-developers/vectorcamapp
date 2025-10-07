package com.vci.vectorcamapp.settings.domain.use_cases

import javax.inject.Inject

data class SettingsValidationUseCases @Inject constructor(
    val validateCollectorTitle: ValidateCollectorTitleUseCase,
    val validateCollectorName: ValidateCollectorNameUseCase,
)
