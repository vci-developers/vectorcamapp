package com.vci.vectorcamapp.core.domain.model.composites

import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.Specimen

data class SessionUnitWithSpecimens(
    val sessionUnit: SessionUnit,
    val specimens: List<Specimen>
)
