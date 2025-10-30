package com.vci.vectorcamapp.registration.presentation.model

import com.vci.vectorcamapp.core.domain.util.collector.CollectorValidationError

data class RegistrationErrors(
    val collectorName: CollectorValidationError? = null,
    val collectorTitle: CollectorValidationError? = null
)
