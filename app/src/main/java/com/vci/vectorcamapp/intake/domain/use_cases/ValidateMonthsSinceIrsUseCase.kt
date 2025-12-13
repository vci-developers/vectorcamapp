package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject

class ValidateMonthsSinceIrsUseCase @Inject constructor() {
    operator fun invoke(count: Int): Result<Unit, FormValidationError> {
        return if (count <= -1) {
            Result.Error(FormValidationError.INVALID_MONTHS_SINCE_IRS)
        } else {
            Result.Success(Unit)
        }
    }
}
