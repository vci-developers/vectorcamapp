package com.vci.vectorcamapp.core.domain.model.composites

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.Session

data class SessionWithFormAnswers(
    val session: Session,
    val formAnswers: List<FormAnswer>
)
