package com.vci.vectorcamapp.core.data.dto.data_recovery

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import java.util.UUID

@Serializable
data class PostDataRecoveryRequestDto(
    val idempotencyKey: String = "",
    val payload: DataRecoveryPayloadDto = DataRecoveryPayloadDto(),
    val programId: Int? = null,
    val siteId: Int? = null,
    @Serializable(with = UuidSerializer::class)
    val sessionFrontendId: UUID = UUID(0, 0),
    val year: Int? = null,
    val month: Int? = null,
    val notes: String? = null,
)

@Serializable
data class DataRecoveryPayloadDto(
    val userId: String = "",
    val deviceId: Int = -1,
    val method: String = "",
    val route: String = "",
    val error: String = "",
    val attemptCount: Int = 0,
    val workerAttempt: Int = 0,
    val failedAt: Long = 0L,
    val requestData: JsonElement = JsonNull,
)
