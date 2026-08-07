package com.vci.vectorcamapp.core.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import com.vci.vectorcamapp.core.logging.analytics.VectorCamAnalytics
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlytics
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlyticsContext
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class CoreViewModel(
    private val errorMessageEmitter: ErrorMessageEmitter
) : ViewModel() {

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

    /**
     * Coroutine launcher with a last-resort exception handler.
     *
     * Catches any unhandled exception that escapes the [block], records it as a non-fatal to
     * Crashlytics, and shows a generic error snackbar so the user is never left in a silent
     * broken state. Each call site should still handle domain-level errors inline; this exists
     * purely as a safety net for truly unexpected throws.
     */
    protected fun safeLaunch(
        screen: String = "",
        feature: String = "",
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            VectorCamCrashlytics.exception(
                throwable = throwable,
                context = VectorCamCrashlyticsContext(
                    screen = screen,
                    feature = feature,
                    action = "safeLaunch"
                )
            )
            emitError(RoomDbError.UNKNOWN_ERROR)
        }
        return viewModelScope.launch(exceptionHandler, block = block)
    }
}
