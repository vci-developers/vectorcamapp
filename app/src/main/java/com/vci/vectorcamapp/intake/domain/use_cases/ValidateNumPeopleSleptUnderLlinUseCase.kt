package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject

class ValidateNumPeopleSleptUnderLlinUseCase @Inject constructor() {
    operator fun invoke(count: Int): Result<Unit, FormValidationError> {
        return if (count == -1) {
            Result.Error(FormValidationError.INVALID_NUM_PEOPLE_SLEPT_UNDER_LLIN)
        } else {
            Result.Success(Unit)
        }
    }
}
