package com.vci.vectorcamapp.collection_batch.domain.util.error

import com.vci.vectorcamapp.core.domain.util.Error

enum class CollectionBatchFormError: Error {
    FORM_INVALID,
    INVALID_FORM_ANSWER,
    CUP_CODE_MISMATCH,
    CUP_NOT_RECORDED,
    INVALID_CUP_LOCATION,
    DUPLICATE_IDENTITY,
    UNKNOWN_ERROR
}
