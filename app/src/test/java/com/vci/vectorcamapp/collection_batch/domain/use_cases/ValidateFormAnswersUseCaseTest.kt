package com.vci.vectorcamapp.collection_batch.domain.use_cases

import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import org.junit.Test
import java.util.UUID

class ValidateFormAnswersUseCaseTest {

    private val useCase = ValidateFormAnswersUseCase()

    @Test
    fun hiddenQuestion_skipsValidation() {
        val gated = question(
            id = 2,
            required = true,
            prerequisite = FormQuestionPrerequisiteExpression.Predicate(
                questionId = 1,
                operator = "eq",
                value = FormQuestionPrerequisiteValue.StringValue("yes"),
            ),
        )

        val results = useCase(
            listOf(question(1), gated),
            mapOf(1 to answer("no"), 2 to answer("")),
        )

        assertThat(results.getValue(2)).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun requiredBlank_isInvalid_optionalBlank_isValid() {
        val results = useCase(
            listOf(question(1, required = true), question(2, required = false)),
            mapOf(1 to answer("  "), 2 to answer("")),
        )

        assertThat(results.getValue(1)).isEqualTo(Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER))
        assertThat(results.getValue(2)).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun number_acceptsNumericValuesAndRejectsMalformedOnes() {
        assertThat(result(question(1, type = "number"), "1.5")).isEqualTo(Result.Success(Unit))
        assertThat(result(question(1, type = "number"), "abc"))
            .isEqualTo(Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER))
        assertThat(result(question(1, type = "number"), ".5"))
            .isEqualTo(Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER))
        assertThat(result(question(1, type = "number"), "5."))
            .isEqualTo(Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER))
    }

    @Test
    fun boolean_acceptsOnlyTrueAndFalse() {
        assertThat(result(question(1, type = "boolean"), "true")).isEqualTo(Result.Success(Unit))
        assertThat(result(question(1, type = "boolean"), "false")).isEqualTo(Result.Success(Unit))
        assertThat(result(question(1, type = "boolean"), "yes"))
            .isEqualTo(Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER))
    }

    @Test
    fun select_mustBeOneOfTheOptionsWhenOptionsExist() {
        val withOptions = question(1, type = "select", options = listOf("A", "B"))
        assertThat(result(withOptions, "A")).isEqualTo(Result.Success(Unit))
        assertThat(result(withOptions, "C"))
            .isEqualTo(Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER))
        assertThat(result(question(1, type = "select", options = null), "anything"))
            .isEqualTo(Result.Success(Unit))
    }

    @Test
    fun freeText_isAccepted() {
        assertThat(result(question(1, type = "text"), "  house  ")).isEqualTo(Result.Success(Unit))
    }

    private fun result(question: FormQuestion, value: String): Result<Unit, CollectionBatchFormError> {
        return useCase(listOf(question), mapOf(question.id to answer(value, question.type))).getValue(question.id)
    }

    private fun question(
        id: Int,
        type: String = "text",
        required: Boolean = false,
        options: List<String>? = null,
        prerequisite: FormQuestionPrerequisiteExpression? = null,
    ) = FormQuestion(
        id = id,
        label = "q$id",
        type = type,
        required = required,
        prerequisite = prerequisite,
        options = options,
        order = id,
        answerScope = FormQuestionScope.SESSION_UNIT,
        isUnitIdentityComponent = false,
    )

    private fun answer(value: String, type: String = "text") = FormAnswer(
        localId = UUID.randomUUID(),
        remoteId = null,
        value = value,
        dataType = type,
        submittedAt = 0L,
    )
}
