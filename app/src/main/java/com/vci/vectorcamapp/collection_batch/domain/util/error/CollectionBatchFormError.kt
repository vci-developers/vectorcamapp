package com.vci.vectorcamapp.collection_batch.domain.util.error

import com.vci.vectorcamapp.core.domain.util.Error

enum class CollectionBatchFormError: Error {
    FORM_INVALID,
    INVALID_FORM_ANSWER,
    DUPLICATE_IDENTITY,
    UNKNOWN_ERROR
}
