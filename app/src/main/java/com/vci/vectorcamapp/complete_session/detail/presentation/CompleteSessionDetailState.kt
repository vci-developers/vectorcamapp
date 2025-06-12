package com.vci.vectorcamapp.complete_session.detail.presentation

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import java.util.UUID

data class CompleteSessionDetailState (
    val session: Session = Session(
        id = UUID(0, 0),
        createdAt = 0L,
        submittedAt = null),
    val surveillanceForm: SurveillanceForm = SurveillanceForm(
        country = "",
        district = "",
        healthCenter = "",
        sentinelSite = "",
        householdNumber = "",
        latitude = 0F,
        longitude = 0F,
        collectionDate = System.currentTimeMillis(),
        collectionMethod = "",
        collectorName = "",
        collectorTitle = "",
        numPeopleSleptInHouse = 0,
        wasIrsConducted = false,
        monthsSinceIrs = null,
        numLlinsAvailable = 0,
        llinType = null,
        llinBrand = null,
        numPeopleSleptUnderLlin = null,
        notes = ""
    ),
    val error: String? = null
)
