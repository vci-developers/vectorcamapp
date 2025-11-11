package com.vci.vectorcamapp.core.domain.use_cases.collector

import javax.inject.Inject

data class CollectorValidationUseCases @Inject constructor(
    val validateCollectorTitle: ValidateCollectorTitleUseCase,
    val validateCollectorName: ValidateCollectorNameUseCase,
    val validateCollectorLastTrainedOn: ValidateCollectorLastTrainedOnUseCase
)
