package com.vci.vectorcamapp.imaging.presentation

import com.vci.vectorcamapp.imaging.domain.util.ImagingError

sealed interface ImagingEvent {
    data class DisplayImagingError(val error: ImagingError) : ImagingEvent
    data object NavigateBackToLandingScreen : ImagingEvent
}
