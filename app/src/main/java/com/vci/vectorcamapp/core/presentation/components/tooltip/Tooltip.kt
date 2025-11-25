package com.vci.vectorcamapp.core.presentation.components.tooltip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun Tooltip(
    isVisible: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    buttonText: String? = null,
    confirmText: String = "Done",
    iconSize: Dp = MaterialTheme.dimensions.iconSizeSmall,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)
){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = "Tooltip Icon",
            tint = MaterialTheme.colors.icon,
            modifier = Modifier
                .size(iconSize)
        )
        if (buttonText != null) {
            Text(
                text = buttonText,
                style = textStyle,
                color = MaterialTheme.colors.textSecondary,
                modifier = Modifier.padding(start = MaterialTheme.dimensions.spacingSmall)
            )
        }
    }

    if (!isVisible) return

    AlertDialog(
        onDismissRequest = { onDismiss() },
        text = {
            Column {
                Spacer(modifier = Modifier.size(MaterialTheme.dimensions.spacingSmall))
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(
                    text = confirmText,
                    color = MaterialTheme.colors.icon,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
            }
        }
    )
}
