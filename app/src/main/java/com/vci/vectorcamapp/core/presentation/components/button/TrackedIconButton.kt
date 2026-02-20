package com.vci.vectorcamapp.core.presentation.components.button

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.core.logging.CrashyContext

/**
 * [IconButton] that sends a Sentry breadcrumb via [ClickTracking] when clicked.
 */
@Composable
fun TrackedIconButton(
    message: String,
    crashyContext: CrashyContext,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = {
            ClickTracking.trackAndInvoke(
                context = crashyContext,
                message = message,
                testTag = testTag,
                onClick = onClick
            )
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}
