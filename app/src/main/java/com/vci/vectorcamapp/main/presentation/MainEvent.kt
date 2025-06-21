package com.vci.vectorcamapp.main.presentation

sealed interface MainEvent {
    object LaunchPermissionRequest : MainEvent
    object NavigateToAppSettings : MainEvent
    object NavigateToLocationSettings : MainEvent
}