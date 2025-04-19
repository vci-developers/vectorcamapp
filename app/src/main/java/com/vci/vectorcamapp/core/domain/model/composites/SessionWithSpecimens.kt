package com.vci.vectorcamapp.core.domain.model.composites

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Specimen

data class SessionWithSpecimens(
    val session: Session,
    val specimens: List<Specimen>
)
