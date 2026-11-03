package com.vci.vectorcamapp.core.presentation.model

import androidx.compose.material3.SnackbarDuration
import com.vci.vectorcamapp.core.presentation.util.success.SuccessMessage

data class SuccessData(
    val message: SuccessMessage,
    val duration: SnackbarDuration = SnackbarDuration.Long
)
