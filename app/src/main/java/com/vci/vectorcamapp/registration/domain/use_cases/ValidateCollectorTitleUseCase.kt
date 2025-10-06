package com.vci.vectorcamapp.registration.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.registration.domain.util.RegistrationValidationError
import javax.inject.Inject

class ValidateCollectorTitleUseCase @Inject constructor() {
    operator fun invoke(collectorTitle: String) : Result<Unit, RegistrationValidationError> {
        return if (collectorTitle.isBlank()) {
            Result.Error(RegistrationValidationError.BLANK_COLLECTOR_TITLE)
        } else {
            Result.Success(Unit)
        }
    }
}
