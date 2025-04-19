package com.vci.vectorcamapp.core.domain.model

import java.util.UUID

data class Session(
    val id: UUID,
    val createdAt: Long,
    val submittedAt: Long?,
)
