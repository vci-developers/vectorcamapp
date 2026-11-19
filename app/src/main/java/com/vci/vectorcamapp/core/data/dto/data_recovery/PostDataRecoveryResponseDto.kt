package com.vci.vectorcamapp.core.data.dto.data_recovery

import kotlinx.serialization.Serializable

@Serializable
data class PostDataRecoveryResponseDto(
    val message: String? = null,
    val duplicate: Boolean = false,
    val dataRecovery: DataRecoveryDto? = null,
)

@Serializable
data class DataRecoveryDto(
    val id: Int = -1,
    val idempotencyKey: String = "",
    val status: String? = null,
)
