package com.vci.vectorcamapp.intake.domain.strategy.program_form

import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm

interface ProgramFormWorkflow {
    val surveillanceForm: SurveillanceForm?
    val form: Form?
    val formQuestions: List<FormQuestion>
}
