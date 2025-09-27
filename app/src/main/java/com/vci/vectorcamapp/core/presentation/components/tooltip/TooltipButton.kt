package com.vci.vectorcamapp.core.presentation.components.tooltip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
fun TooltipButton(
    onClick: () -> Unit,
    text: String? = null,
    iconSize: Dp = MaterialTheme.dimensions.iconSizeSmall,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = "Tooltip Icon",
            tint = MaterialTheme.colors.icon,
            modifier = Modifier
                .size(iconSize)
        )
        if (text != null) {
            Text(
                text = text,
                style = textStyle,
                color = MaterialTheme.colors.textSecondary,
                modifier = Modifier.padding(start = MaterialTheme.dimensions.spacingSmall)
            )
        }
    }
}
