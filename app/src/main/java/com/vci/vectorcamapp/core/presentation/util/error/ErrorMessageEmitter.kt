package com.vci.vectorcamapp.core.presentation.util.error

import androidx.compose.material3.SnackbarDuration
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.model.ErrorData
import kotlinx.coroutines.flow.SharedFlow

interface ErrorMessageEmitter {
    val errors: SharedFlow<ErrorData>

    suspend fun emit(error: Error, duration: SnackbarDuration = SnackbarDuration.Long)

    fun clearLastMessage()
}
