package com.vci.vectorcamapp.main.presentation

import com.vci.vectorcamapp.navigation.Destination

data class MainState(
    val allPermissionsGranted: Boolean = false,
    val isGpsEnabled: Boolean = false,
    val selectedProgramName: String? = null,
    val isLoading: Boolean = true,
    val startDestination: Destination? = null
)