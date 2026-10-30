package com.vci.vectorcamapp.core.presentation.util.success

import androidx.compose.material3.SnackbarDuration
import com.vci.vectorcamapp.core.presentation.model.SuccessData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

class DefaultSuccessMessageEmitter @Inject constructor() : SuccessMessageEmitter {

    private val _messages = MutableSharedFlow<SuccessData>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val messages = _messages.asSharedFlow()

    private var lastKey: String? = null

    override suspend fun emit(message: SuccessMessage, duration: SnackbarDuration) {
        val key = message.name
        if (key != lastKey) {
            lastKey = key
            _messages.emit(SuccessData(message, duration))
        }
    }

    override fun clearLastMessage() {
        lastKey = null
    }
}
