package com.vci.vectorcamapp.core.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun SearchHelpTooltipContent() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)) {
        Text(
            text = "How Search Works",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = MaterialTheme.dimensions.paddingSmall)
        )
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)) {
            Text(
                text = "• Put a SPACE between words when you want results that include all the words.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textPrimary
            )
            Text(
                text = "• Put a COMMA between groups when any one group is okay.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textPrimary
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)) {
            Text(
                text = "Examples:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colors.textSecondary,
            )
            Text(
                text = "• Anopheles Male → must include Anopheles and Male.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textSecondary
            )
            Text(
                text = "• Anopheles Male, Mansonia Female → either (Anopheles and Male) or (Mansonia and Female).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textSecondary
            )
        }
    }
}
