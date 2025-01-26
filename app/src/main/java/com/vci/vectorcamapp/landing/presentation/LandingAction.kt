package com.vci.vectorcamapp.landing.presentation

sealed interface LandingAction {
    data object StartNewSurveillanceSession : LandingAction
    data object StartNewNonSurveillanceSession : LandingAction
    data object ViewIncompleteSessions : LandingAction
    data object ViewCompleteSessions : LandingAction
}
