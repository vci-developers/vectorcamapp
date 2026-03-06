package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject

class ValidateVillageNameUseCase @Inject constructor() {
    operator fun invoke(villageName: String): Result<Unit, FormValidationError> {
        return if (villageName.isBlank()) {
            Result.Error(FormValidationError.BLANK_VILLAGE_NAME)
        } else {
            Result.Success(Unit)
        }
    }
}
