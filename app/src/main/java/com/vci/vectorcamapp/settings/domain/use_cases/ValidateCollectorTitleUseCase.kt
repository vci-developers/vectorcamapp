package com.vci.vectorcamapp.settings.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.settings.domain.util.SettingsValidationError
import javax.inject.Inject

class ValidateCollectorTitleUseCase @Inject constructor() {
    operator fun invoke(collectorTitle: String) : Result<Unit, SettingsValidationError> {
        return if (collectorTitle.isBlank()) {
            Result.Error(SettingsValidationError.BLANK_COLLECTOR_TITLE)
        } else {
            Result.Success(Unit)
        }
    }
}
