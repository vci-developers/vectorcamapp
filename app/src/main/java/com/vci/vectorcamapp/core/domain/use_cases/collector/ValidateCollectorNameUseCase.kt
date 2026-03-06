package com.vci.vectorcamapp.core.domain.use_cases.collector

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.collector.CollectorValidationError
import javax.inject.Inject

class ValidateCollectorNameUseCase @Inject constructor() {
    operator fun invoke(collectorName: String): Result<Unit, CollectorValidationError> {
        return if (collectorName.isBlank()) {
            Result.Error(CollectorValidationError.BLANK_COLLECTOR_NAME)
        } else {
            Result.Success(Unit)
        }
    }
}
