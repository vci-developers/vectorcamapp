package com.vci.vectorcamapp.collection_batch.form.presentation

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import java.util.UUID

data class CollectionBatchFormState(
    val isLoading: Boolean = true,
    val sessionId: UUID = UUID(0, 0),
    val sessionUnitId: UUID? = null,
    val formQuestions: List<FormQuestion> = emptyList(),
    val formAnswersByQuestionId: Map<Int, FormAnswer> = emptyMap()
)
