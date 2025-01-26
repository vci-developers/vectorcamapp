package com.vci.vectorcamapp.permission.presentation

sealed interface PermissionAction {
    data object RequestPermissions : PermissionAction
    data object OpenAppSettings : PermissionAction
    data object OpenLocationSettings : PermissionAction
    data class UpdatePermissionStatus(val allGranted: Boolean) : PermissionAction
    data class UpdateGpsStatus(val isGpsEnabled: Boolean) : PermissionAction
}
