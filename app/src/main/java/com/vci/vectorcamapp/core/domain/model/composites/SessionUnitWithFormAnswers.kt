package com.vci.vectorcamapp.core.domain.model.composites

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.SessionUnit

data class SessionUnitWithFormAnswers(
    val sessionUnit: SessionUnit,
    val formAnswers: List<FormAnswer>
)
