package com.vci.vectorcamapp.intake.domain.strategy.concrete

import com.vci.vectorcamapp.core.domain.model.AnswerScopes
import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.intake.domain.strategy.ProgramFormWorkflow

class FormPresentWorkflow(
    override val form: Form,
    questions: List<FormQuestion>,
) : ProgramFormWorkflow {
    // SESSION_UNIT questions belong to CollectionBatchForm, not the intake form
    override val formQuestions: List<FormQuestion> =
        questions.filter { it.answerScope != AnswerScopes.SESSION_UNIT }
    override val surveillanceForm: SurveillanceForm?
        get() = null
}
