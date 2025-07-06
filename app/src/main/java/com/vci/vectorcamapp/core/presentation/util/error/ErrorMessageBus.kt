package com.vci.vectorcamapp.core.presentation.util.error

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.vci.vectorcamapp.core.domain.util.Error
import kotlinx.coroutines.channels.BufferOverflow

object ErrorMessageBus {
    private val _errors = MutableSharedFlow<ErrorMessage>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errors = _errors.asSharedFlow()

    private var lastKey: String? = null

    suspend fun emit(error: Error, context: Context, duration: SnackbarDuration) {
        val key = "${error::class.simpleName}-${error.hashCode()}"
        if (key != lastKey) {
            lastKey = key
            _errors.emit(ErrorMessage(error.toString(context), duration))
        }
    }

    fun clearLastMessage() {
        lastKey = null
    }
}
