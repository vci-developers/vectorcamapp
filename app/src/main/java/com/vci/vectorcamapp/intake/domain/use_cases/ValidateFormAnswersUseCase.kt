package com.vci.vectorcamapp.intake.domain.use_cases

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import javax.inject.Inject

class ValidateFormAnswersUseCase @Inject constructor() {
    operator fun invoke(
        formQuestions: List<FormQuestion>,
        formAnswers: Map<Int, FormAnswer>
    ): Map<Int, Result<Unit, FormValidationError>> {
        return formQuestions.associate { question ->
            val answer = formAnswers[question.id]?.value.orEmpty().trim()

            val formAnswerResult = when {
                question.required && answer.isBlank() -> Result.Error(FormValidationError.INVALID_FORM_ANSWER)

                answer.isBlank() -> Result.Success(Unit)

                else -> when (question.type) {
                    "number" -> if (answer.toIntOrNull() == null) {
                        Result.Error(FormValidationError.INVALID_FORM_ANSWER)
                    } else Result.Success(Unit)

                    "boolean" -> if (answer != "true" && answer != "false") {
                        Result.Error(FormValidationError.INVALID_FORM_ANSWER)
                    } else Result.Success(Unit)

                    "select" -> if (question.options != null && answer !in question.options) {
                        Result.Error(FormValidationError.INVALID_FORM_ANSWER)
                    } else Result.Success(Unit)

                    else -> Result.Success(Unit)
                }
            }

            question.id to formAnswerResult
        }
    }
}
