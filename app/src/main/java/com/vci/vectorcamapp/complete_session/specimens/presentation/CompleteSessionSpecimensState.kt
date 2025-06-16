package com.vci.vectorcamapp.complete_session.specimens.presentation

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Specimen
import java.util.UUID

data class CompleteSessionSpecimensState (
    val session: Session = Session(
        id = UUID(0, 0),
        createdAt = 0L,
        submittedAt = null
    ),
    val specimens: List<Specimen> = emptyList(),
    val error: String? = null
)
