package com.vci.vectorcamapp.imaging.presentation

import java.util.UUID

sealed interface ImagingEvent {
    data object NavigateBackToLandingScreen : ImagingEvent
    data class NavigateBackToCollectionBatchListScreen(val sessionId: UUID) : ImagingEvent
}
