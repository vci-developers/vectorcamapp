package com.vci.vectorcamapp.collection_batch.domain.use_cases

import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
import javax.inject.Inject

class ValidateFormAnswersUseCase @Inject constructor() {
    operator fun invoke(
        formQuestions: List<FormQuestion>,
        formAnswersByQuestionId: Map<Int, FormAnswer>
    ): Map<Int, Result<Unit, CollectionBatchFormError>> {
        val answerMap = formAnswersByQuestionId.mapValues { (_, answer) -> answer.value.trim() }

        return formQuestions.associate { question ->
            if (!FormQuestionPrerequisiteEvaluator.evaluate(question.prerequisite, answerMap)) {
                return@associate question.id to Result.Success(Unit)
            }

            val answer = formAnswersByQuestionId[question.id]?.value.orEmpty().trim()

            val formAnswerResult = when {
                question.required && answer.isBlank() -> Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER)

                answer.isBlank() -> Result.Success(Unit)

                else -> when (question.type) {
                    "number" -> {
                        if (answer.toDoubleOrNull() == null || answer.startsWith(".") || answer.endsWith(".")) {
                            Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER)
                        } else Result.Success(Unit)
                    }

                    "boolean" -> if (answer != "true" && answer != "false") {
                        Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER)
                    } else Result.Success(Unit)

                    "select" -> if (question.options != null && answer !in question.options) {
                        Result.Error(CollectionBatchFormError.INVALID_FORM_ANSWER)
                    } else Result.Success(Unit)

                    else -> Result.Success(Unit)
                }
            }

            question.id to formAnswerResult
        }
    }
}