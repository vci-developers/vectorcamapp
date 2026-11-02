package com.vci.vectorcamapp.core.presentation.components.scaffold

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.screenWidthFraction

@Composable
fun FeedbackSnackbar(
    message: String,
    containerColor: Color,
    contentColor: Color,
    icon: Painter,
    iconContentDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthFraction = 0.96f

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall),
        shadowElevation = MaterialTheme.dimensions.shadowOffsetMedium,
        modifier = modifier.widthIn(max = screenWidthFraction(widthFraction))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.dimensions.paddingLarge,
                    vertical = MaterialTheme.dimensions.paddingMedium
                )
        ) {
            Icon(
                painter = icon,
                contentDescription = iconContentDescription,
                tint = contentColor,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
            )
            Spacer(modifier = Modifier.widthIn(min = MaterialTheme.dimensions.spacingSmall))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.widthIn(min = MaterialTheme.dimensions.spacingMedium))
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.core_content_description_dismiss),
                tint = MaterialTheme.colors.textSecondary,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeExtraSmall)
                    .clickable(onClick = onDismiss)
            )
        }
    }
}
