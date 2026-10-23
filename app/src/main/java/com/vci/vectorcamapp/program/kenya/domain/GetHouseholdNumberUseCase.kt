package com.vci.vectorcamapp.program.kenya.domain
 
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import java.util.UUID
import javax.inject.Inject
 
/** Reads the historical question attached to the answer, not the program's latest form. */
class GetHouseholdNumberUseCase @Inject constructor(
    private val formRepository: FormRepository,
) {
    suspend operator fun invoke(sessionId: UUID): String? =
        formRepository.getFormsWithFormAnswersAndQuestionsBySessionId(sessionId)
            .flatMap { it.formAnswersAndQuestions }
            .singleOrNull { it.question.questionKey == KenyaFormRules.HOUSEHOLD_NUMBER_KEY }
            ?.answer?.value?.let { (it as? FormAnswerValue.StringValue)?.value }
            ?.trim()?.takeIf { it.isNotEmpty() }
}