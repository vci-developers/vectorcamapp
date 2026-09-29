package com.vci.vectorcamapp.collection_batch.domain.use_cases

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.util.Result
import org.junit.Test
import java.util.UUID

class ValidateCollectionBatchIdentityUseCaseTest {

    private val useCase = ValidateCollectionBatchIdentityUseCase()
    private val editingId = UUID.fromString("00000000-0000-0000-0000-000000000010")
    private val otherId = UUID.fromString("00000000-0000-0000-0000-000000000011")

    @Test
    fun noIdentityQuestions_isSuccess() {
        val result = useCase(
            formQuestions = listOf(question(1, identity = false)),
            draftAnswersByQuestionId = mapOf(1 to answer("A")),
            existingAnswersBySessionUnitId = emptyMap(),
            editingSessionUnitId = editingId,
        )

        assertThat(result).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun incompleteDraft_isSuccess() {
        val result = useCase(
            formQuestions = listOf(question(1), question(2)),
            draftAnswersByQuestionId = mapOf(1 to answer("A"), 2 to answer("  ")),
            existingAnswersBySessionUnitId = mapOf(otherId to mapOf(1 to answer("A"), 2 to answer("B"))),
            editingSessionUnitId = editingId,
        )

        assertThat(result).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun matchingOtherUnit_isDuplicate() {
        val result = useCase(
            formQuestions = listOf(question(1), question(2), question(3, identity = false)),
            draftAnswersByQuestionId = mapOf(
                1 to answer(" House "),
                2 to answer("12"),
                3 to answer("ignored"),
            ),
            existingAnswersBySessionUnitId = mapOf(
                otherId to mapOf(1 to answer("House"), 2 to answer(" 12 ")),
            ),
            editingSessionUnitId = editingId,
        )

        assertThat(result).isEqualTo(Result.Error(CollectionBatchFormError.DUPLICATE_IDENTITY))
    }

    @Test
    fun matchingOnlyTheUnitBeingEdited_isSuccess() {
        val result = useCase(
            formQuestions = listOf(question(1)),
            draftAnswersByQuestionId = mapOf(1 to answer("House")),
            existingAnswersBySessionUnitId = mapOf(editingId to mapOf(1 to answer("House"))),
            editingSessionUnitId = editingId,
        )

        assertThat(result).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun partialMatch_isSuccess() {
        val result = useCase(
            formQuestions = listOf(question(1), question(2)),
            draftAnswersByQuestionId = mapOf(1 to answer("House"), 2 to answer("12")),
            existingAnswersBySessionUnitId = mapOf(otherId to mapOf(1 to answer("House"), 2 to answer("13"))),
            editingSessionUnitId = null,
        )

        assertThat(result).isEqualTo(Result.Success(Unit))
    }

    private fun question(id: Int, identity: Boolean = true) = FormQuestion(
        id = id,
        label = "q$id",
        type = "text",
        required = true,
        prerequisite = null,
        options = null,
        order = id,
        answerScope = if (identity) FormQuestionScope.SESSION_UNIT else FormQuestionScope.SESSION,
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
