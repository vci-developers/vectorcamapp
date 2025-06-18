package com.vci.vectorcamapp.complete_session.details.presentation

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import java.util.UUID

data class CompleteSessionDetailsState (
    val session: Session = Session(
        localId = UUID.randomUUID(),
        remoteId = null,
        houseNumber = "",
        collectorTitle = "",
        collectorName = "",
        collectionDate = System.currentTimeMillis(),
        collectionMethod = "",
        specimenCondition = "",
        createdAt = System.currentTimeMillis(),
        completedAt = null,
        submittedAt = null,
        notes = ""
    ),
    val surveillanceForm: SurveillanceForm = SurveillanceForm(
        numPeopleSleptInHouse = 0,
        wasIrsConducted = false,
        monthsSinceIrs = null,
        numLlinsAvailable = 0,
        llinType = null,
        llinBrand = null,
        numPeopleSleptUnderLlin = null
    ),
    val error: String? = null
)
