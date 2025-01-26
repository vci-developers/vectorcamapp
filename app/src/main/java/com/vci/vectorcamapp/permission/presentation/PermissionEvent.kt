package com.vci.vectorcamapp.permission.presentation

sealed interface PermissionEvent {
    data object LaunchPermissionRequest : PermissionEvent
    data object NavigateToAppSettings : PermissionEvent
    data object NavigateToLocationSettings: PermissionEvent
}
