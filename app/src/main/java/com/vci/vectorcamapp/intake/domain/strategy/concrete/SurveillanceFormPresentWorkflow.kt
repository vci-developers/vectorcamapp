package com.vci.vectorcamapp.intake.domain.strategy.concrete

import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.intake.domain.strategy.SurveillanceFormWorkflow

class SurveillanceFormPresentWorkflow : SurveillanceFormWorkflow {
    override fun createNewSurveillanceForm(): SurveillanceForm {
        return SurveillanceForm(
            numPeopleSleptInHouse = -1,
            wasIrsConducted = false,
            monthsSinceIrs = null,
            numLlinsAvailable = -1,
            llinType = null,
            llinBrand = null,
            numPeopleSleptUnderLlin = null,
            submittedAt = null
        )
    }
}
