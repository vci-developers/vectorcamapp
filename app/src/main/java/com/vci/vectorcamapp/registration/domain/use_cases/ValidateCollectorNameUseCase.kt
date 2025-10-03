package com.vci.vectorcamapp.registration.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import com.vci.vectorcamapp.registration.domain.util.RegistrationValidationError
import javax.inject.Inject

class ValidateCollectorNameUseCase @Inject constructor() {
    operator fun invoke(collectorName: String) : Result<Unit, RegistrationValidationError> {
        return if (collectorName.isBlank()) {
            Result.Error(RegistrationValidationError.BLANK_COLLECTOR_NAME)
        } else {
            Result.Success(Unit)
        }
    }
}
