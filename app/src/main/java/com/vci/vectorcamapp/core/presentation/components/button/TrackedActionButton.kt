package com.vci.vectorcamapp.core.presentation.components.button

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import com.vci.vectorcamapp.core.logging.CrashyContext
import com.vci.vectorcamapp.core.presentation.LocalCrashyContext

/**
 * [ActionButton] that sends a Sentry breadcrumb via [ClickTracking] when clicked.
 * Uses [LocalCrashyContext] when [crashyContext] is null (e.g. set in [VectorcamappTheme] or screen provider).
 */
@Composable
fun TrackedActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconPainter: Painter? = null,
    textSize: TextStyle = MaterialTheme.typography.bodyLarge,
    testTag: String? = null,
    crashyContext: CrashyContext? = null,
    feature: String? = null,
    action: String? = null
) {
    val resolvedContext = (crashyContext ?: LocalCrashyContext.current)?.let { base ->
        if (feature != null || action != null) base.copy(feature = feature ?: base.feature, action = action ?: base.action)
        else base
    }
    ActionButton(
        label = label,
        onClick = {
            if (resolvedContext != null) {
                ClickTracking.trackAndInvoke(
                    context = resolvedContext,
                    message = "Button clicked: $label",
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
        iconPainter = iconPainter,
        textSize = textSize,
        testTag = testTag
    )
}

