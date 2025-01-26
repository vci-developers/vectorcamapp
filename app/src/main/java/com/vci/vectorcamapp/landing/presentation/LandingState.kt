package com.vci.vectorcamapp.landing.presentation

import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.core.domain.model.ImagingSession

data class LandingState(
    val versionName: String = BuildConfig.VERSION_NAME,
    val isLoading: Boolean = false,
    val incompleteSessions: List<ImagingSession> = emptyList(),
    val completeSessions: List<ImagingSession> = emptyList(),
)
