package com.vci.vectorcamapp.landing.presentation

sealed interface LandingAction {
    data object StartNewSurveillanceSession : LandingAction
    data object ViewIncompleteSessions : LandingAction
    data object ViewCompleteSessions : LandingAction
    data object OpenSettings : LandingAction
    data object ResumeSession : LandingAction
    data object DismissResumePrompt : LandingAction
    data object RefreshSites : LandingAction
}
