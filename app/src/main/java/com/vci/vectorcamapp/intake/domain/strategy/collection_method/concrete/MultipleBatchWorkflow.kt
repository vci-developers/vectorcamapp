package com.vci.vectorcamapp.intake.domain.strategy.collection_method.concrete

import com.vci.vectorcamapp.intake.domain.strategy.collection_method.CollectionMethodWorkflow
import com.vci.vectorcamapp.navigation.Destination
import java.util.UUID

class MultipleBatchWorkflow(sessionId: UUID) : CollectionMethodWorkflow {
    override val postIntakeDestination: Destination =
        Destination.HourLog(sessionId = sessionId.toString())
}
