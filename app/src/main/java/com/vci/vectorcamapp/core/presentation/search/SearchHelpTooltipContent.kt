package com.vci.vectorcamapp.core.presentation.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun SearchHelpTooltipContent() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)) {
        Text(
            text = stringResource(R.string.search_title_help),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = MaterialTheme.dimensions.paddingSmall)
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)) {
            Text(
                text = stringResource(R.string.search_body_space_rule),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textPrimary
            )
            Text(
                text = stringResource(R.string.search_body_comma_rule),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textPrimary
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)) {
            Text(
                text = stringResource(R.string.search_label_examples),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colors.textSecondary,
            )
            Text(
                text = stringResource(R.string.search_body_example_1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textSecondary
            )
            Text(
                text = stringResource(R.string.search_body_example_2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textSecondary
            )
        }
    }
}
