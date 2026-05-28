package com.vci.vectorcamapp.collection_batch.form.presentation

import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.intake.domain.util.FormValidationError

data class CollectionBatchFormState(
    val isLoading: Boolean = true,
    val sessionId: String = "",
    val editingUnitId: String? = null,
    val identityQuestions: List<FormQuestion> = emptyList(),
    val otherUnitQuestions: List<FormQuestion> = emptyList(),
    val answers: Map<Int, String> = emptyMap(),
    val errorsByQuestionId: Map<Int, FormValidationError> = emptyMap(),
    val duplicateIdentityError: String? = null,
)
