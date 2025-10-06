package com.vci.vectorcamapp.settings.presentation

interface SettingsAction {
    data object StartNewDataCollectionSession : SettingsAction
    data object ReturnToLandingScreen : SettingsAction
}
