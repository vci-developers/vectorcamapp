package com.vci.vectorcamapp.collection_batch.domain.util

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import org.junit.Test
import java.util.UUID

class CollectionBatchIdentityResolverTest {

    @Test
    fun deriveBucketName_joinsIdentityAnswersInQuestionIdOrder() {
        val questions = listOf(
            question(id = 3, label = "later"),
            question(id = 1, label = "first", identity = false),
            question(id = 2, label = "second", scope = FormQuestionScope.SESSION),
            question(id = 4, label = "blank"),
        )
        val answers = mapOf(
            3 to answer("Trap"),
            1 to answer("ignored"),
            2 to answer("ignored"),
            4 to answer("   "),
        )

        assertThat(CollectionBatchIdentityResolver.deriveBucketName(questions, answers)).isEqualTo("Trap")
    }

    @Test
    fun deriveBucketName_joinsMultipleParts() {
        val questions = listOf(question(2, "house"), question(1, "village"))
        val answers = mapOf(2 to answer(" 12 "), 1 to answer("Kampala"))

        assertThat(CollectionBatchIdentityResolver.deriveBucketName(questions, answers))
            .isEqualTo("Kampala · 12")
    }

    @Test
    fun deriveBucketName_isEmptyWhenNothingIsFilled() {
        assertThat(
            CollectionBatchIdentityResolver.deriveBucketName(listOf(question(1, "village")), emptyMap())
        ).isEmpty()
    }

    private fun question(
        id: Int,
        label: String,
        identity: Boolean = true,
        scope: FormQuestionScope = FormQuestionScope.SESSION_UNIT,
    ) = FormQuestion(
        id = id,
        label = label,
        type = "text",
        required = false,
        prerequisite = null,
        options = null,
        order = id,
        answerScope = scope,
        isUnitIdentityComponent = identity,
    )

    private fun answer(value: String) = FormAnswer(
        localId = UUID.randomUUID(),
        remoteId = null,
        value = value,
        dataType = "text",
        submittedAt = 0L,
    )
}
