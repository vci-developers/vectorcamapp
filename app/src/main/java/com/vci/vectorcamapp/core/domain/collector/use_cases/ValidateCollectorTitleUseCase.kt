package com.vci.vectorcamapp.core.domain.collector.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.collector.CollectorValidationError
import javax.inject.Inject

class ValidateCollectorTitleUseCase @Inject constructor() {
    operator fun invoke(collectorTitle: String) : Result<Unit, CollectorValidationError> {
        return if (collectorTitle.isBlank()) {
            Result.Error(CollectorValidationError.BLANK_COLLECTOR_TITLE)
        } else {
            Result.Success(Unit)
        }
    }
}
