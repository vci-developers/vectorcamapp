package com.vci.vectorcamapp.permission.presentation

data class PermissionState(
    val allGranted: Boolean = false,
    val isGpsEnabled: Boolean = false,
    val isLoading: Boolean = false
)
