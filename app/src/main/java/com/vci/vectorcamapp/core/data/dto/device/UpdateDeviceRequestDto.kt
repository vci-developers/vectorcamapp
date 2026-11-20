package com.vci.vectorcamapp.core.data.dto.device

import kotlinx.serialization.Serializable

@Serializable
data class UpdateDeviceRequestDto(
    val appVersion: String,
)
