package com.vci.vectorcamapp.settings.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.settings.domain.util.SettingsValidationError
import javax.inject.Inject

class ValidateCollectorNameUseCase @Inject constructor() {
    operator fun invoke(collectorName: String) : Result<Unit, SettingsValidationError> {
        return if (collectorName.isBlank()) {
            Result.Error(SettingsValidationError.BLANK_COLLECTOR_NAME)
        } else {
            Result.Success(Unit)
        }
    }
}
