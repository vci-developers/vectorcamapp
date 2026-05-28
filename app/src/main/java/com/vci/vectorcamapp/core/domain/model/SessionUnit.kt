package com.vci.vectorcamapp.core.domain.model

import java.util.UUID

data class SessionUnit(
    val localId: UUID,
    val sessionId: UUID,
    val remoteId: Int?,
    val unitOrder: Int,
    val createdAt: Long,
)
