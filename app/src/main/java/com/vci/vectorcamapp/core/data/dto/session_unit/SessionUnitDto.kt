package com.vci.vectorcamapp.core.data.dto.session_unit

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SessionUnitDto(
    val id: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val frontendId: UUID = UUID(0, 0),
    val sessionId: Int = -1,
    val unitOrder: Int = -1,
    val createdAt: Long = 0L,
)
