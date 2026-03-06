package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject

class ValidateCollectorUseCase @Inject constructor() {
    operator fun invoke(collectorName: String, collectorTitle: String): Result<Unit, FormValidationError> {
        return if (collectorName.isBlank() || collectorTitle.isBlank()) {
            Result.Error(FormValidationError.BLANK_COLLECTOR)
        } else {
            Result.Success(Unit)
        }
    }
}
