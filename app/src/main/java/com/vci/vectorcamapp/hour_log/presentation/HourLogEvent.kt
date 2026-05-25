package com.vci.vectorcamapp.hour_log.presentation

import java.util.UUID

sealed interface HourLogEvent {
    data object NavigateBackToPreviousScreen : HourLogEvent
    data class NavigateToAddHourScreen(val sessionId: String) : HourLogEvent
    data class NavigateToImagingScreen(val hourSessionId: UUID) : HourLogEvent
}
