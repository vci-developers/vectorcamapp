package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject
import com.vci.vectorcamapp.program.kenya.domain.KenyaFormRules

class ValidateFormAnswersUseCase @Inject constructor() {
    operator fun invoke(
        formQuestions: List<FormQuestion>,
        formAnswers: Map<Int, FormAnswer>
    ): Map<Int, Result<Unit, FormValidationError>> {
        val answerMap = formAnswers.mapValues { (_, answer) ->
            when (val value = answer.value) {
                is FormAnswerValue.StringValue -> value.copy(value = value.value.trim())
                is FormAnswerValue.ListValue -> value
            }
        }

        val invalidBreakdownIds = KenyaFormRules.invalidCountBreakdownQuestionIds(formQuestions, formAnswers)
        return formQuestions.associate { question ->
            if (!FormQuestionPrerequisiteEvaluator.evaluate(question.prerequisite, answerMap)) {
                return@associate question.id to Result.Success(Unit)
            }

            val answer = formAnswers[question.id]?.value ?: FormAnswerValue.StringValue("")

            val formAnswerResult = if (question.id in invalidBreakdownIds) {
                Result.Error(FormValidationError.INCONSISTENT_PEOPLE_COUNTS)
            } else if (!KenyaFormRules.hasValidCupLocation(question, answer)) {
                Result.Error(FormValidationError.INVALID_CUP_LOCATION)
            } else if (isValidAnswer(question, answer) &&
                KenyaFormRules.isWithinNumberLimit(question, answer)) {
                Result.Success(Unit)
            } else {
                Result.Error(FormValidationError.INVALID_FORM_ANSWER)
            }

            question.id to formAnswerResult
        }
    }
    private fun isValidAnswer(question: FormQuestion, value: FormAnswerValue): Boolean {
        if (question.type == "multi-select") {
            return when (value) {
                is FormAnswerValue.StringValue -> value.value.isBlank() && !question.required
                is FormAnswerValue.ListValue -> {
                    val selections = value.value
                    (!question.required || selections.isNotEmpty()) &&
                        selections.distinct().size == selections.size &&
                        selections.all { it in question.options.orEmpty() }
                }
            }
        }
        val answer = (value as? FormAnswerValue.StringValue)?.value?.trim() ?: return false
        if (answer.isBlank()) return !question.required
        return when (question.type) {
            "number" -> answer.toDoubleOrNull() != null &&
                !answer.startsWith(".") && !answer.endsWith(".")
            "boolean" -> answer == "true" || answer == "false"
            "select" -> question.options == null || answer in question.options
            else -> true
        }
    }
}
