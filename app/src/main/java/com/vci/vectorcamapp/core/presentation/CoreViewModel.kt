package com.vci.vectorcamapp.core.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.logging.analytics.VectorCamAnalytics
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.presentation.util.success.SuccessMessage
import com.vci.vectorcamapp.core.presentation.util.success.SuccessMessageEmitter
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class CoreViewModel : ViewModel() {

    /**
     * Field-injected so every screen shares these emitters without another constructor parameter.
     * Hilt sets them after construction, before the ViewModel is used.
     */
    @Inject
    lateinit var errorMessageEmitter: ErrorMessageEmitter

    @Inject
    lateinit var successMessageEmitter: SuccessMessageEmitter

    /**
     * Emits a user-visible error snackbar AND fires a generic GA4 `error_emitted` event.
     *
     * This gives 100% error-event coverage across every screen without any per-feature wiring.
     * Per-feature `*_failed` events can still be added alongside calls to this method for
     * more targeted funnel filtering.
     */
    protected fun emitError(
        error: Error, duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        VectorCamAnalytics.logEvent(
            name = "error_emitted",
            params = mapOf(
                "error_class" to error::class.simpleName,
                "error_name" to (error as? Enum<*>)?.name,
            )
        )
        viewModelScope.launch {
            errorMessageEmitter.emit(error, duration)
        }
    }

    protected fun emitSuccess(
        message: SuccessMessage,
        duration: SnackbarDuration = SnackbarDuration.Long,
    ) {
        viewModelScope.launch {
            successMessageEmitter.emit(message, duration)
        }
    }
}
