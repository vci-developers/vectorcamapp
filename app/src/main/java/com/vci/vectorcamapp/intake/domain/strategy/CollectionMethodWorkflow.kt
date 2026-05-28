package com.vci.vectorcamapp.intake.domain.strategy

import com.vci.vectorcamapp.navigation.Destination

interface CollectionMethodWorkflow {
    /** Returns the destination the Intake screen should navigate to after saving the session. */
    fun postIntakeDestination(sessionId: String): Destination
}
