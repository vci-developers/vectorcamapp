package com.vci.vectorcamapp.core.domain.collector.use_cases

import javax.inject.Inject

data class CollectorValidationUseCases @Inject constructor(
    val validateCollectorTitle: ValidateCollectorTitleUseCase,
    val validateCollectorName: ValidateCollectorNameUseCase,
)
