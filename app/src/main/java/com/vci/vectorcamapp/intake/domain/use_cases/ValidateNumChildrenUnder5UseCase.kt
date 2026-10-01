package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject

class ValidateNumChildrenUnder5UseCase @Inject constructor() {
    operator fun invoke(count: Int?): Result<Unit, FormValidationError> {
        return if (count == null || count <= -1) {
            Result.Error(FormValidationError.INVALID_NUM_CHILDREN_UNDER_5)
        } else {
            Result.Success(Unit)
        }
    }
}
