package com.vci.vectorcamapp.add_hour.presentation

sealed interface AddHourEvent {
    data object NavigateBackToPreviousScreen : AddHourEvent
    data object NavigateToImagingScreen : AddHourEvent
}
