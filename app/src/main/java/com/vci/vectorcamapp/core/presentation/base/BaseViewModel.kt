package com.vci.vectorcamapp.core.presentation.base

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageBus
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    protected fun emitError(
        error: Error,
        context: Context,
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        viewModelScope.launch {
            ErrorMessageBus.emit(error, context, duration)
        }
    }
}
