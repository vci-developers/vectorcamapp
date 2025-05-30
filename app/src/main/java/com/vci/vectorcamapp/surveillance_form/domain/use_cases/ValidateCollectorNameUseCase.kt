package com.vci.vectorcamapp.surveillance_form.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.surveillance_form.domain.util.FormValidationError
import javax.inject.Inject

class ValidateCollectorNameUseCase @Inject constructor() {
    operator fun invoke(collectorName: String) : Result<Unit, FormValidationError> {
        return if (collectorName.isBlank()) {
            Result.Error(FormValidationError.BLANK_COLLECTOR_NAME)
        } else {
            Result.Success(Unit)
        }
    }
}
