package com.vci.vectorcamapp.intake.domain.strategy

import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions.CollectionMethodOption
import com.vci.vectorcamapp.intake.domain.strategy.concrete.DirectImagingWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.concrete.RepeatableUnitWorkflow
import javax.inject.Inject

class CollectionMethodWorkflowFactory @Inject constructor() {
    fun create(collectionMethod: String): CollectionMethodWorkflow {
        return when (collectionMethod) {
            CollectionMethodOption.HUMAN_LANDING_CATCH.label -> RepeatableUnitWorkflow()
            else -> DirectImagingWorkflow()
        }
    }
}
