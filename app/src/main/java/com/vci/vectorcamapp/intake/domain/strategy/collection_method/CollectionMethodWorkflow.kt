package com.vci.vectorcamapp.intake.domain.strategy.collection_method

import com.vci.vectorcamapp.navigation.Destination

interface CollectionMethodWorkflow {
    val postIntakeDestination: Destination
}
