package com.vci.vectorcamapp.core.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import kotlinx.coroutines.launch

abstract class CoreViewModel(
    private val errorMessageEmitter: ErrorMessageEmitter
) : ViewModel() {
    protected fun emitError(
        error: Error, duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        viewModelScope.launch {
            errorMessageEmitter.emit(error, duration)
        }
    }
}
