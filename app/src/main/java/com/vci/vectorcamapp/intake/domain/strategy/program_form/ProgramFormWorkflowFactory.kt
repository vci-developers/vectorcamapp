package com.vci.vectorcamapp.intake.domain.strategy.program_form

import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.intake.domain.strategy.program_form.concrete.FormPresentWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.program_form.concrete.IdentityOnlyFormWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.program_form.concrete.SurveillanceFormPresentWorkflow
import javax.inject.Inject

class ProgramFormWorkflowFactory @Inject constructor(
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository,
) {
    suspend fun create(sessionType: SessionType, program: Program): ProgramFormWorkflow {
        val form = program.formVersion?.let { formRepository.getFormByVersion(it) }

        return when (sessionType) {
            SessionType.DATA_COLLECTION -> IdentityOnlyFormWorkflow(
                allSessionUnitFormQuestions = questionsForScope(form, FormQuestionScope.SESSION_UNIT)
            )

            SessionType.SURVEILLANCE,
            SessionType.PRACTICE -> if (form != null) {
                FormPresentWorkflow(
                    form = form,
                    sessionFormQuestions = questionsForScope(form, FormQuestionScope.SESSION),
                    sessionUnitFormQuestions = questionsForScope(
                        form,
                        FormQuestionScope.SESSION_UNIT,
                    ),
                )
            } else {
                SurveillanceFormPresentWorkflow()
            }
        }
    }

    private suspend fun questionsForScope(
        form: Form?,
        answerScope: FormQuestionScope,
    ): List<FormQuestion> = form?.let {
        formQuestionRepository.getQuestionsByFormIdAndScope(
            formId = it.id,
            answerScope = answerScope,
        )
    }.orEmpty()
}
