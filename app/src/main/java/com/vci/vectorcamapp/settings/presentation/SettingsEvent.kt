package com.vci.vectorcamapp.settings.presentation

import com.vci.vectorcamapp.core.domain.model.enums.SessionType

sealed interface SettingsEvent {
    data class NavigateToIntakeScreen(val sessionType: SessionType) : SettingsEvent
    data object NavigateBackToLandingScreen : SettingsEvent
}
