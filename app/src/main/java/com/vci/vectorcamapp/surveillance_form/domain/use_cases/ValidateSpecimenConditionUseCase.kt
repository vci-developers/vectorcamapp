package com.vci.vectorcamapp.surveillance_form.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.surveillance_form.domain.util.FormValidationError
import javax.inject.Inject

class ValidateSpecimenConditionUseCase @Inject constructor() {
    operator fun invoke(specimenCondition: String): Result<Unit, FormValidationError> {
        return if (specimenCondition.isBlank()) {
            Result.Error(FormValidationError.BLANK_SPECIMEN_CONDITION)
        } else {
            Result.Success(Unit)
        }
    }
}
