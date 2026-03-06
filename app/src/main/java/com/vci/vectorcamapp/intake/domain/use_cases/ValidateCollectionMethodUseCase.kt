package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject

class ValidateCollectionMethodUseCase @Inject constructor() {
    operator fun invoke(collectionMethod: String): Result<Unit, FormValidationError> {
        if (collectionMethod.isBlank()) return Result.Error(FormValidationError.BLANK_COLLECTION_METHOD)

        if (collectionMethod.startsWith(IntakeDropdownOptions.CollectionMethodOption.OTHER.label, ignoreCase = true)) {
            val suffix = collectionMethod.substringAfter(
                IntakeDropdownOptions.CollectionMethodOption.OTHER.label
            ).trim()
            if (suffix.isBlank()) return Result.Error(FormValidationError.BLANK_COLLECTION_METHOD)
        }

        return Result.Success(Unit)
    }
}
