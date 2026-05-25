package com.vci.vectorcamapp.hour_log.presentation

import java.util.UUID

sealed interface HourLogAction {
    data object ReturnToPreviousScreen : HourLogAction
    data object NavigateToAddHour : HourLogAction
    data class ResumeHourSession(val hourSessionId: UUID) : HourLogAction
    data object UploadSession : HourLogAction
}
