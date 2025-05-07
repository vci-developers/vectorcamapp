package com.vci.vectorcamapp.surveillance_form.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.surveillance_form.domain.util.FormValidationError
import javax.inject.Inject

class ValidateCountryUseCase @Inject constructor() {
    operator fun invoke(country: String) : Result<Unit, FormValidationError> {
        return if (country.isBlank()) {
            Result.Error(FormValidationError.BLANK_COUNTRY)
        } else {
            Result.Success(Unit)
        }
    }
}
