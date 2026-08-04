package com.vci.vectorcamapp.intake.domain.strategy.collection_method

import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.domain.strategy.collection_method.concrete.MultipleBatchWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.collection_method.concrete.SingleBatchWorkflow
import java.util.UUID
import javax.inject.Inject

class CollectionMethodWorkflowFactory @Inject constructor() {
    fun create(sessionId: UUID, collectionMethod: String): CollectionMethodWorkflow {
        return SingleBatchWorkflow()
    }
}
