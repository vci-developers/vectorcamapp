package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ValidateFormAnswersUseCaseTest {

    private lateinit var useCase: ValidateFormAnswersUseCase

    // region helpers

    private fun question(
        id: Int,
        type: String = "text",
        required: Boolean = false,
        prerequisite: FormQuestionPrerequisiteExpression? = null,
        options: List<String>? = null
    ) = FormQuestion(
        id = id,
        label = "Question $id",
        type = type,
        required = required,
        prerequisite = prerequisite,
        options = options,
        order = id
    )

    private fun answer(questionId: Int, value: String): Pair<Int, FormAnswer> =
        questionId to FormAnswer(
            localId = UUID.randomUUID(),
            remoteId = null,
            value = value,
            dataType = "text",
            submittedAt = System.currentTimeMillis()
        )

    // endregion

    @Before
    fun setUp() {
        useCase = ValidateFormAnswersUseCase()
    }

    // region a - Required field validation

    @Test
    fun requiredField_withAnswer_returnsSuccess() {
        val questions = listOf(question(id = 1, required = true))
        val answers = mapOf(answer(1, "some value"))
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
    }

    @Test
    fun requiredField_withBlankAnswer_returnsError() {
        val questions = listOf(question(id = 1, required = true))
        val answers = mapOf(answer(1, "   "))
        val result = useCase(questions, answers)
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[1] as Result.Error).error)
    }

    @Test
    fun requiredField_withNoAnswer_returnsError() {
        val questions = listOf(question(id = 1, required = true))
        val result = useCase(questions, emptyMap())
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[1] as Result.Error).error)
    }

    @Test
    fun optionalField_withNoAnswer_returnsSuccess() {
        val questions = listOf(question(id = 1, required = false))
        val result = useCase(questions, emptyMap())
        assertTrue(result[1] is Result.Success)
    }

    @Test
    fun optionalField_withBlankAnswer_returnsSuccess() {
        val questions = listOf(question(id = 1, required = false))
        val answers = mapOf(answer(1, "  "))
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
    }

    // endregion

    // region b - Type: number

    @Test
    fun numberField_validInteger_returnsSuccess() {
        val questions = listOf(question(id = 1, type = "number"))
        val answers = mapOf(answer(1, "42"))
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
    }

    @Test
    fun numberField_validDecimal_returnsSuccess() {
        val questions = listOf(question(id = 1, type = "number"))
        val answers = mapOf(answer(1, "3.14"))
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
    }

    @Test
    fun numberField_invalidText_returnsError() {
        val questions = listOf(question(id = 1, type = "number"))
        val answers = mapOf(answer(1, "hello"))
        val result = useCase(questions, answers)
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[1] as Result.Error).error)
    }

    @Test
    fun numberField_tooManyDecimalPlaces_returnsError() {
        val questions = listOf(question(id = 1, type = "number"))
        val answers = mapOf(answer(1, "3.141"))
        val result = useCase(questions, answers)
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[1] as Result.Error).error)
    }

    @Test
    fun numberField_trailingDot_returnsError() {
        val questions = listOf(question(id = 1, type = "number"))
        val answers = mapOf(answer(1, "5."))
        val result = useCase(questions, answers)
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[1] as Result.Error).error)
    }

    @Test
    fun numberField_leadingDot_returnsError() {
        val questions = listOf(question(id = 1, type = "number"))
        val answers = mapOf(answer(1, ".5"))
        val result = useCase(questions, answers)
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[1] as Result.Error).error)
    }

    // endregion

    // region c - Type: boolean

    @Test
    fun booleanField_trueString_returnsSuccess() {
        val questions = listOf(question(id = 1, type = "boolean"))
        val answers = mapOf(answer(1, "true"))
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
    }

    @Test
    fun booleanField_falseString_returnsSuccess() {
        val questions = listOf(question(id = 1, type = "boolean"))
        val answers = mapOf(answer(1, "false"))
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
    }

    @Test
    fun booleanField_invalidString_returnsError() {
        val questions = listOf(question(id = 1, type = "boolean"))
        val answers = mapOf(answer(1, "yes"))
        val result = useCase(questions, answers)
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[1] as Result.Error).error)
    }

    // endregion

    // region d - Type: select

    @Test
    fun selectField_validOption_returnsSuccess() {
        val questions = listOf(question(id = 1, type = "select", options = listOf("A", "B", "C")))
        val answers = mapOf(answer(1, "B"))
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
    }

    @Test
    fun selectField_invalidOption_returnsError() {
        val questions = listOf(question(id = 1, type = "select", options = listOf("A", "B", "C")))
        val answers = mapOf(answer(1, "D"))
        val result = useCase(questions, answers)
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[1] as Result.Error).error)
    }

    @Test
    fun selectField_noOptions_anyAnswerReturnsSuccess() {
        val questions = listOf(question(id = 1, type = "select", options = null))
        val answers = mapOf(answer(1, "anything"))
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
    }

    // endregion

    // region e - Prerequisite: question skipped when prerequisite not met

    @Test
    fun prerequisiteNotMet_questionSkipped_returnsSuccess() {
        val prerequisite = FormQuestionPrerequisiteExpression.Predicate(
            questionId = 1,
            operator = "eq",
            value = FormQuestionPrerequisiteValue.StringValue("yes")
        )
        val questions = listOf(
            question(id = 1, required = true),
            question(id = 2, required = true, prerequisite = prerequisite)
        )
        // Question 1 answered with "no" → prerequisite for question 2 fails → question 2 skipped
        val answers = mapOf(answer(1, "no"))
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success) // question 1 answered "no" → non-blank, type=text → success
        assertTrue(result[2] is Result.Success) // question 2 is skipped because prerequisite not met
    }

    @Test
    fun prerequisiteMet_questionValidated_blankRequiredAnswer_returnsError() {
        val prerequisite = FormQuestionPrerequisiteExpression.Predicate(
            questionId = 1,
            operator = "eq",
            value = FormQuestionPrerequisiteValue.StringValue("yes")
        )
        val questions = listOf(
            question(id = 1, required = true),
            question(id = 2, required = true, prerequisite = prerequisite)
        )
        val answers = mapOf(answer(1, "yes"))
        // question 1 answered "yes" → prerequisite for question 2 is met → question 2 required but no answer
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[2] as Result.Error).error)
    }

    @Test
    fun noPrerequisite_questionAlwaysValidated() {
        val questions = listOf(question(id = 1, required = true))
        val result = useCase(questions, emptyMap())
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[1] as Result.Error).error)
    }

    // endregion

    // region f - Multiple questions validated together

    @Test
    fun multipleQuestions_allValid_allReturnSuccess() {
        val questions = listOf(
            question(id = 1, required = true, type = "text"),
            question(id = 2, required = true, type = "number"),
            question(id = 3, required = true, type = "boolean")
        )
        val answers = mapOf(
            answer(1, "hello"),
            answer(2, "10"),
            answer(3, "true")
        )
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
        assertTrue(result[2] is Result.Success)
        assertTrue(result[3] is Result.Success)
    }

    @Test
    fun multipleQuestions_someInvalid_mixedResults() {
        val questions = listOf(
            question(id = 1, required = true, type = "text"),
            question(id = 2, required = true, type = "number")
        )
        val answers = mapOf(
            answer(1, "hello"),
            answer(2, "not-a-number")
        )
        val result = useCase(questions, answers)
        assertTrue(result[1] is Result.Success)
        assertEquals(FormValidationError.INVALID_FORM_ANSWER, (result[2] as Result.Error).error)
    }

    @Test
    fun emptyQuestions_returnsEmptyMap() {
        val result = useCase(emptyList(), emptyMap())
        assertTrue(result.isEmpty())
    }

    // endregion
}
