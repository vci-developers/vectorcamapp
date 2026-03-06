package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun PracticeWarningBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colors.warningBackground
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = MaterialTheme.colors.warning,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
            )
            Text(
                text = "This is a practice session. Submitted data will not be considered routine surveillance data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
