package com.vci.vectorcamapp.core.presentation.util.error

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object ErrorMessageBus {
    private val _errors = MutableSharedFlow<ErrorMessage>(extraBufferCapacity = 1)
    val errors: SharedFlow<ErrorMessage> = _errors

    fun emit(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        _errors.tryEmit(ErrorMessage(message, duration))
    }
}
