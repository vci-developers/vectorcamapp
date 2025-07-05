package com.vci.vectorcamapp.core.presentation.util.error

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.vci.vectorcamapp.core.domain.util.Error

object ErrorMessageBus {
    private val _errors = MutableSharedFlow<ErrorMessage>(extraBufferCapacity = 64)
    val errors = _errors.asSharedFlow()

    private var lastMessage: String? = null

    suspend fun emit(message: String, duration: SnackbarDuration = SnackbarDuration.Long) {
        if (message != lastMessage) {
            lastMessage = message
            _errors.emit(ErrorMessage(message, duration))
        }
    }

    suspend fun emit(error: Error, context: Context, duration: SnackbarDuration = SnackbarDuration.Long) {
        emit(error.toString(context), duration)
    }

    fun clearLastMessage() {
        lastMessage = null
    }
}
