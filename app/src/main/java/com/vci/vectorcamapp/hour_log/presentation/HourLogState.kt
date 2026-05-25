package com.vci.vectorcamapp.hour_log.presentation

import com.vci.vectorcamapp.hour_log.domain.model.HourSession

data class HourLogState(
    val isLoading: Boolean = false,
    val sessionId: String = "",
    val hourSessions: List<HourSession> = emptyList()
)
