package com.vci.vectorcamapp.hour_log.domain.model

import java.util.UUID

data class HourSession(
    val id: UUID = UUID.randomUUID(),
    val parentSessionId: UUID,
    val timeSlot: String,
    val specimenCount: Int = 0,
    val wind: String = "",
    val rain: String = "",
    val relativeHumidity: String = "",
    val temperature: String = "",
    val collectionPlace: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
