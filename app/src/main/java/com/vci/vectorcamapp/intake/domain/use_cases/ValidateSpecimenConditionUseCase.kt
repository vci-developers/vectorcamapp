package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject

class ValidateSpecimenConditionUseCase @Inject constructor() {
    operator fun invoke(specimenCondition: String): Result<Unit, FormValidationError> {
        if (specimenCondition.isBlank()) return Result.Error(FormValidationError.BLANK_SPECIMEN_CONDITION)

        if (specimenCondition.startsWith(
                IntakeDropdownOptions.SpecimenConditionOption.OTHER.label,
                ignoreCase = true
            )) {
            val suffix = specimenCondition.substringAfter(
                IntakeDropdownOptions.SpecimenConditionOption.OTHER.label
            ).trim()
            if (suffix.isBlank()) return Result.Error(FormValidationError.BLANK_SPECIMEN_CONDITION)
        }

        return Result.Success(Unit)
    }
}
