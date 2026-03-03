package com.vci.vectorcamapp.incomplete_session.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun IncompleteSessionListDetailRow(
    iconPainter: Painter?, iconDescription: String, text: String, modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        if (iconPainter != null) {
            Icon(
                painter = iconPainter,
                contentDescription = iconDescription,
                tint = MaterialTheme.colors.icon,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
            )
        } else {
            Spacer(modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall))
        }

        Spacer(modifier = Modifier.size(MaterialTheme.dimensions.spacingSmall))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colors.textPrimary
        )
    }
}
