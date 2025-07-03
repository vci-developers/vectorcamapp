package com.vci.vectorcamapp.core.presentation.base

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageBus
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    protected fun emitError(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        viewModelScope.launch {
            ErrorMessageBus.emit(message, duration)
        }
    }
}