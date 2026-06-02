package com.vci.vectorcamapp.collection_batch.domain.util

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope

object CollectionBatchIdentityResolver {

    private const val IDENTITY_SEPARATOR = " · "

    fun deriveBucketName(
        formQuestions: List<FormQuestion>, answersByQuestionId: Map<Int, FormAnswer>
    ): String {
        return formQuestions.filter { it.answerScope == FormQuestionScope.SESSION_UNIT && it.isUnitIdentityComponent }
            .sortedBy { it.id }.mapNotNull { question ->
                answersByQuestionId[question.id]?.value?.trim()?.takeIf { it.isNotBlank() }
            }.joinToString(IDENTITY_SEPARATOR)
    }
}