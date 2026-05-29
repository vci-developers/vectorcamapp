package com.vci.vectorcamapp.core.domain.model.composites

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SessionUnit

data class SessionWithSessionUnits(
    val session: Session,
    val sessionUnits: List<SessionUnit>
)
