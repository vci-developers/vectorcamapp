package com.vci.vectorcamapp.intake.domain.strategy.concrete

import com.vci.vectorcamapp.intake.domain.strategy.CollectionMethodWorkflow
import com.vci.vectorcamapp.navigation.Destination

class RepeatableUnitWorkflow : CollectionMethodWorkflow {
    override fun postIntakeDestination(sessionId: String): Destination =
        Destination.CollectionBatchList(sessionId = sessionId)
}
