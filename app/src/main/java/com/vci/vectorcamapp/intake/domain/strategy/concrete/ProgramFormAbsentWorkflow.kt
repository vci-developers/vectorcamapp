package com.vci.vectorcamapp.intake.domain.strategy.concrete

import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.intake.domain.strategy.ProgramFormWorkflow

class ProgramFormAbsentWorkflow : ProgramFormWorkflow {
    override val surveillanceForm: SurveillanceForm?
        get() = null

    override val form: Form?
        get() = null

    override val formQuestions: List<FormQuestion>
        get() = emptyList()
}
