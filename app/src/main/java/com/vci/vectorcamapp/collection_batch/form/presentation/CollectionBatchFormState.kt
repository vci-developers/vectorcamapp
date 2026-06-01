package com.vci.vectorcamapp.collection_batch.form.presentation

import com.vci.vectorcamapp.collection_batch.form.presentation.model.CollectionBatchFormErrors
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion

data class CollectionBatchFormState(
    val isLoading: Boolean = true,
    val formQuestions: List<FormQuestion> = emptyList(),
    val formAnswersByQuestionId: Map<Int, FormAnswer> = emptyMap(),
    val collectionBatchFormErrors: CollectionBatchFormErrors = CollectionBatchFormErrors(
        duplicateIdentity = null,
        formAnswerErrors = emptyMap()
    )
)
