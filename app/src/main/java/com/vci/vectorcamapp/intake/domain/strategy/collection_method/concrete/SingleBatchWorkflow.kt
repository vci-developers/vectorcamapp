package com.vci.vectorcamapp.intake.domain.strategy.collection_method.concrete

import com.vci.vectorcamapp.intake.domain.strategy.collection_method.CollectionMethodWorkflow
import com.vci.vectorcamapp.navigation.Destination

class SingleBatchWorkflow : CollectionMethodWorkflow {
    override val postIntakeDestination: Destination = Destination.Imaging(sessionUnitId = null)
}
