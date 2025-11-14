package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject

class ValidateHardwareIdUseCase @Inject constructor() {
    operator fun invoke(hardwareId: String): Result<Unit, FormValidationError> {
        return if (hardwareId.isBlank()) {
            Result.Error(FormValidationError.BLANK_HARDWARE_ID)
        } else {
            Result.Success(Unit)
        }
    }
}