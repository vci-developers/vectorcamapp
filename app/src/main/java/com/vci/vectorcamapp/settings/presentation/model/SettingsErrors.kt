package com.vci.vectorcamapp.settings.presentation.model

import com.vci.vectorcamapp.core.domain.util.collector.CollectorValidationError

data class SettingsErrors(
    val collectorName: CollectorValidationError? = null,
    val collectorTitle: CollectorValidationError? = null
)
