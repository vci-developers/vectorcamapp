package com.vci.vectorcamapp.core.presentation.util.error

import androidx.compose.material3.SnackbarDuration

data class ErrorMessage(
    val message: String,
    val duration: SnackbarDuration = SnackbarDuration.Long
)
