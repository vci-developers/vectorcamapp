package com.vci.vectorcamapp.core.presentation.components.button

import com.vci.vectorcamapp.core.logging.Crashy
import com.vci.vectorcamapp.core.logging.CrashyContext
import io.sentry.SentryLevel

/**
 * Shared delegate for recording a Sentry breadcrumb on click and then invoking the action.
 * Used by [TrackedActionButton], [TrackedTextButton], and [TrackedIconButton].
 */
object ClickTracking {

    private const val DEFAULT_CATEGORY = "ui.button.click"

    /**
     * Records a breadcrumb with [context] and [message], then runs [onClick].
     * Use this to wrap any click handler that should be tracked in Sentry.
     */
    fun trackAndInvoke(
        context: CrashyContext,
        message: String,
        category: String = DEFAULT_CATEGORY,
        level: SentryLevel = SentryLevel.INFO,
        buttonLabel: String? = null,
        testTag: String? = null,
        onClick: () -> Unit
    ) {
        Crashy.globalBreadcrumb(
            message = message,
            category = category,
            level = level,
            data = buildMap<String, Any?> {
                buttonLabel?.let { put("button_label", it) }
                testTag?.let { put("test_tag", it) }
            },
            context = context
        )
        onClick()
    }
}
