package com.vci.vectorcamapp.surveillance_form.domain.use_cases

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.surveillance_form.domain.util.FormValidationError
import javax.inject.Inject

class ValidateCollectionDateUseCase @Inject constructor() {
    operator fun invoke(collectionDate: Long) : Result<Unit, FormValidationError> {
        return if (collectionDate > System.currentTimeMillis()) {
            Result.Error(FormValidationError.FUTURE_COLLECTION_DATE)
        } else {
            Result.Success(Unit)
        }
    }
}
