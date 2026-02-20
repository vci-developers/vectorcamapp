package com.vci.vectorcamapp.core.presentation.components.button

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import com.vci.vectorcamapp.core.logging.CrashyContext

@Composable
fun TrackedActionButton(
    label: String,
    crashyContext: CrashyContext,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconPainter: Painter? = null,
    textSize: TextStyle = MaterialTheme.typography.bodyLarge,
    testTag: String? = null
) {
    ActionButton(
        label = label,
        onClick = {
            ClickTracking.trackAndInvoke(
                context = crashyContext,
                message = "Button clicked: $label",
                buttonLabel = label,
                testTag = testTag,
                onClick = onClick
            )
        },
        modifier = modifier,
        enabled = enabled,
        iconPainter = iconPainter,
        textSize = textSize,
        testTag = testTag
    )
}

