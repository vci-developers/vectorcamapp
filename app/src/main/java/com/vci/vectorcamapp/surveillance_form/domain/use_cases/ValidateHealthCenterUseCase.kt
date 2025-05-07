package com.vci.vectorcamapp.surveillance_form.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.surveillance_form.domain.util.FormValidationError
import javax.inject.Inject

class ValidateHealthCenterUseCase @Inject constructor() {
    operator fun invoke(healthCenter: String) : Result<Unit, FormValidationError> {
        return if (healthCenter.isBlank()) {
            Result.Error(FormValidationError.BLANK_HEALTH_CENTER)
        } else {
            Result.Success(Unit)
        }
    }
}
