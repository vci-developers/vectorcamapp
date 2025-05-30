package com.vci.vectorcamapp.surveillance_form.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.surveillance_form.domain.util.FormValidationError
import javax.inject.Inject

class ValidateSentinelSiteUseCase @Inject constructor() {
    operator fun invoke(sentinelSite: String) : Result<Unit, FormValidationError> {
        return if (sentinelSite.isBlank()) {
            Result.Error(FormValidationError.BLANK_SENTINEL_SITE)
        } else {
            Result.Success(Unit)
        }
    }
}
