package com.vci.vectorcamapp.collection_batch.domain.util

import com.vci.vectorcamapp.core.domain.model.FormQuestion

object CollectionBatchIdentityResolver {

    private const val SEPARATOR = " · "

    /**
     * Returns the derived bucket name for a unit, given the unit's answers keyed by questionId
     * and the form's questions. Considers only questions where
     * answerScope = SESSION_UNIT AND isUnitIdentityComponent = true, sorted by questionId.
     *
     * Returns an empty string if no identity components are present (caller falls back to
     * e.g. "Batch ${unitOrder}").
     */
    fun deriveBucketName(
        questions: List<FormQuestion>,
        unitAnswersByQuestionId: Map<Int, String>,
    ): String {
        val identityQuestions = questions
            .filter { it.answerScope == "SESSION_UNIT" && it.isUnitIdentityComponent }
            .sortedBy { it.id }

        if (identityQuestions.isEmpty()) return ""

        return identityQuestions.joinToString(SEPARATOR) { q ->
            normalize(unitAnswersByQuestionId[q.id].orEmpty())
        }
    }

    private fun normalize(value: String): String = value.trim()
}
