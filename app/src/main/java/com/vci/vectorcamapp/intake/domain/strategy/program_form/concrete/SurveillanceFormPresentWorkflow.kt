package com.vci.vectorcamapp.intake.domain.strategy.program_form.concrete

import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.intake.domain.strategy.program_form.ProgramFormWorkflow

class SurveillanceFormPresentWorkflow : ProgramFormWorkflow {
    override val surveillanceForm: SurveillanceForm
        get() = SurveillanceForm(
            numPeopleSleptInHouse = -1,
            wasIrsConducted = false,
            monthsSinceIrs = null,
            numLlinsAvailable = -1,
            llinType = null,
            llinBrand = null,
            numPeopleSleptUnderLlin = null,
            submittedAt = null
        )

    override val form: Form?
        get() = null

    override val formQuestions: List<FormQuestion>
        get() = emptyList()
}
