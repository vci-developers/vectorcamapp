package com.vci.vectorcamapp.core.presentation.util.success

import androidx.compose.material3.SnackbarDuration
import com.vci.vectorcamapp.core.presentation.model.SuccessData
import kotlinx.coroutines.flow.SharedFlow

interface SuccessMessageEmitter {
    val messages: SharedFlow<SuccessData>

    suspend fun emit(message: SuccessMessage, duration: SnackbarDuration = SnackbarDuration.Long)

    fun clearLastMessage()
}
