package com.vci.vectorcamapp.core.presentation.components.tooltip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun TooltipDialogRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    iconPainter: Painter? = null,
    iconDescription: String = "",
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
        modifier = modifier.fillMaxWidth()
    ) {
        if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = iconDescription,
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .padding(end = MaterialTheme.dimensions.spacingSmall)
            )
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colors.textPrimary)) {
                    if (title != null) {
                        append("$title")
                        if (description != null) {
                            append(": ")
                        }
                    }
                }
                withStyle(SpanStyle(color = MaterialTheme.colors.textSecondary)) {
                    if (description != null) {
                    append("$description")
                        }
                }
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
