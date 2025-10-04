package com.vci.vectorcamapp.settings.presentation.model

import com.vci.vectorcamapp.settings.domain.util.SettingsValidationError

data class SettingsErrors(
    val collectorName: SettingsValidationError? = null,
    val collectorTitle: SettingsValidationError? = null
)
