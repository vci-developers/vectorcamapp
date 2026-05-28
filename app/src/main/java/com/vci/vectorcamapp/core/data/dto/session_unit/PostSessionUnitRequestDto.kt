package com.vci.vectorcamapp.core.data.dto.session_unit

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PostSessionUnitRequestDto(
    @Serializable(with = UuidSerializer::class)
    val frontendId: UUID,
    val unitOrder: Int,
    val createdAt: Long,
)
