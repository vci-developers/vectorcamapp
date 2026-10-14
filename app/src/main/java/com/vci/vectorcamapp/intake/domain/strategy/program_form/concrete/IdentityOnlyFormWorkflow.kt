package com.vci.vectorcamapp.intake.domain.strategy.program_form.concrete

import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.intake.domain.strategy.program_form.ProgramFormWorkflow

class IdentityOnlyFormWorkflow(
    allSessionUnitFormQuestions: List<FormQuestion>,
) : ProgramFormWorkflow {
    override val surveillanceForm: SurveillanceForm?
        get() = null

    override val form: Form?
        get() = null

    override val sessionFormQuestions: List<FormQuestion>
        get() = emptyList()

    override val sessionUnitFormQuestions: List<FormQuestion> =
        allSessionUnitFormQuestions.filter { it.isUnitIdentityComponent }
}
