package com.vci.vectorcamapp.complete_session.specimens.presentation

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Specimen
import java.util.UUID

data class CompleteSessionSpecimensState (
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
    val specimens: List<Specimen> = emptyList(),
    val error: String? = null
)
