package com.vci.vectorcamapp.core.data.room.helpers

import java.util.UUID

data class SessionCounts(
    val sessionId: UUID,
    val uploadedImages: Int,
    val totalImages: Int
)
