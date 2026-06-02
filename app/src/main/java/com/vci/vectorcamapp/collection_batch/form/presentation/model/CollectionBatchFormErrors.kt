package com.vci.vectorcamapp.collection_batch.form.presentation.model

import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError

data class CollectionBatchFormErrors(
    val duplicateIdentity: CollectionBatchFormError?,
    val formAnswerErrors: Map<Int, CollectionBatchFormError?>
)
