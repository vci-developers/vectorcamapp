package com.vci.vectorcamapp.core.presentation.base

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.complete_session.form.presentation.CompleteSessionFormState
import com.vci.vectorcamapp.complete_session.specimens.presentation.CompleteSessionSpecimensState
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageBus
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    protected fun emitError(message: String, duration: SnackbarDuration = SnackbarDuration.Short): CompleteSessionSpecimensState {
        viewModelScope.launch {
            ErrorMessageBus.emit(message, duration)
        }
    }
}