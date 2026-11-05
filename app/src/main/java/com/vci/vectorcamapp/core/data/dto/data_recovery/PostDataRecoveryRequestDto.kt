package com.vci.vectorcamapp.core.data.dto.data_recovery

import com.vci.vectorcamapp.core.data.dto.serializers.UuidSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import java.util.UUID

@Serializable
data class PostDataRecoveryRequestDto(
    val userId: String = "",
    val deviceId: Int = -1,
    val programId: Int = -1,
    @Serializable(with = UuidSerializer::class)
    val sessionFrontendId: UUID = UUID(0, 0),
    val method: String = "",
    val route: String = "",
    val error: String = "",
    val attemptCount: Int = 0,
    val workerAttempt: Int = 0,
    val requestData: JsonElement = JsonNull,
    val failedAt: Long = 0L,
)
