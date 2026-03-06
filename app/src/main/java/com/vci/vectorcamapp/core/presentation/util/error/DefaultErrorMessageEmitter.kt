package com.vci.vectorcamapp.core.presentation.util.error

import androidx.compose.material3.SnackbarDuration
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.model.ErrorData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

class DefaultErrorMessageEmitter @Inject constructor() : ErrorMessageEmitter {

    private val _errors = MutableSharedFlow<ErrorData>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val errors = _errors.asSharedFlow()

    private var lastKey: String? = null

    override suspend fun emit(error: Error, duration: SnackbarDuration) {
        val key = "${error::class.simpleName}-${error.hashCode()}"
        if (key != lastKey) {
            lastKey = key
            _errors.emit(ErrorData(error, duration))
        }
    }

    override fun clearLastMessage() {
        lastKey = null
    }
}
