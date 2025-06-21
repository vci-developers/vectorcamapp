package com.vci.vectorcamapp.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val id: Int? = null,
    val model: String = "",
    val registeredAt: Long = 0L,
    val programName: String = ""
) {
    fun isEmpty() = id == null
}