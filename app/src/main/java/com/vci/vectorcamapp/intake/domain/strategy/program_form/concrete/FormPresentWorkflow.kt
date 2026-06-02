package com.vci.vectorcamapp.intake.domain.strategy.program_form.concrete

import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.intake.domain.strategy.program_form.ProgramFormWorkflow

class FormPresentWorkflow(
    override val form: Form, override val formQuestions: List<FormQuestion>
) : ProgramFormWorkflow {
    override val surveillanceForm: SurveillanceForm?
        get() = null
}
