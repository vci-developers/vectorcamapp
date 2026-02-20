package com.vci.vectorcamapp.core.presentation.components.button

import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.core.logging.CrashyContext
import com.vci.vectorcamapp.core.presentation.LocalCrashyContext

/**
 * [TextButton] that sends a Sentry breadcrumb via [ClickTracking] when clicked.
 * Uses [LocalCrashyContext] when [crashyContext] is null (e.g. set in [VectorcamappTheme] or screen provider).
 */
@Composable
fun TrackedTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
    crashyContext: CrashyContext? = null,
    feature: String? = null,
    action: String? = null,
    content: @Composable () -> Unit
) {
    val resolvedContext = (crashyContext ?: LocalCrashyContext.current)?.let { base ->
        if (feature != null || action != null) base.copy(feature = feature ?: base.feature, action = action ?: base.action)
        else base
    }
    TextButton(
        onClick = {
            if (resolvedContext != null) {
                ClickTracking.trackAndInvoke(
                    context = resolvedContext,
                    message = "TextButton clicked: $label",
                    buttonLabel = label,
                    testTag = testTag,
                    onClick = onClick
                )
            } else {
                onClick()
            }
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}
