package com.vci.vectorcamapp.core.data.dto

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SessionDto(
    @Serializable(with = UuidSerializer::class)
    val id: UUID = UUID(0, 0),
    val createdAt: Long = 0L,
    val submittedAt: Long? = null,
) {
    fun isEmpty() = id == UUID(0, 0) && createdAt == 0L
}
