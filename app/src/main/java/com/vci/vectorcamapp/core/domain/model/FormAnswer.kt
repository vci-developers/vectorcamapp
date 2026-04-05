package com.vci.vectorcamapp.core.domain.model

import java.util.UUID

data class FormAnswer(
    val localId: UUID,
    val remoteId: Int?,
    val value: String,
    val dataType: String,
    val submittedAt: Long
)
