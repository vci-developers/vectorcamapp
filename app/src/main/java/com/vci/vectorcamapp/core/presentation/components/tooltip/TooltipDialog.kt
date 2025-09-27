package com.vci.vectorcamapp.core.presentation.components.tooltip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun TooltipDialog(
    isVisible: Boolean,
    title: String? = null,
    content: @Composable (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = MaterialTheme.dimensions.paddingSmall)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
            ) {
                if (content != null) {
                    content()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(
                    "Done",
                    color = MaterialTheme.colors.icon,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
            }
        }
    )
}
