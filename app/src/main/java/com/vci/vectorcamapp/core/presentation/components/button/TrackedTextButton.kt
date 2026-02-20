package com.vci.vectorcamapp.core.presentation.components.button

import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.core.logging.CrashyContext

/**
 * [TextButton] that sends a Sentry breadcrumb via [ClickTracking] when clicked.
 */
@Composable
fun TrackedTextButton(
    label: String,
    crashyContext: CrashyContext,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    TextButton(
        onClick = {
            ClickTracking.trackAndInvoke(
                context = crashyContext,
                message = "TextButton clicked: $label",
                buttonLabel = label,
                testTag = testTag,
                onClick = onClick
            )
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}
