package com.vci.vectorcamapp.surveillance_form.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.surveillance_form.domain.util.FormValidationError
import javax.inject.Inject

class ValidateHouseholdNumberUseCase @Inject constructor() {
    operator fun invoke(householdNumber: String) : Result<Unit, FormValidationError> {
        return if (householdNumber.isBlank()) {
            Result.Error(FormValidationError.BLANK_HOUSEHOLD_NUMBER)
        } else {
            Result.Success(Unit)
        }
    }
}
