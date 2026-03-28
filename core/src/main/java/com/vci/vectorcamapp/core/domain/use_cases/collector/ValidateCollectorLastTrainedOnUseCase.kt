package com.vci.vectorcamapp.core.domain.use_cases.collector

import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.collector.CollectorValidationError
import javax.inject.Inject

class ValidateCollectorLastTrainedOnUseCase @Inject constructor() {
    operator fun invoke(collectionDate: Long) : Result<Unit, CollectorValidationError> {
        return if (collectionDate > System.currentTimeMillis() || collectionDate <= 0L) {
            Result.Error(CollectorValidationError.INVALID_LAST_TRAINED_ON_DATE)
        } else {
            Result.Success(Unit)
        }
    }
}
