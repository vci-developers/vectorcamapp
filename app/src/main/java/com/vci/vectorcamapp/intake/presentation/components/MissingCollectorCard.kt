package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
internal fun MissingCollectorCard(
    name: String,
    title: String,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimensions.paddingSmall),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colors.appBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = MaterialTheme.dimensions.paddingLarge,
                    bottom = MaterialTheme.dimensions.paddingMedium,
                )
                .padding(horizontal = MaterialTheme.dimensions.paddingMedium),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)) {
                MissingCollectorWarning()
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)) {
                    CollectorDetail(label = stringResource(R.string.intake_label_name), value = name)
                    CollectorDetail(label = stringResource(R.string.intake_label_title), value = title)
                }
            }
            Spacer(Modifier.height(MaterialTheme.dimensions.spacingSmall))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.dimensions.paddingSmall),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onRegister,
                    shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.buttonText,
                    ),
                ) {
                    Text(text = stringResource(R.string.intake_action_register_collector))
                }
            }
        }
    }
}

@Composable
private fun MissingCollectorWarning() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = stringResource(R.string.intake_content_description_missing_collector),
                tint = MaterialTheme.colors.error,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium),
            )
            Text(
                text = stringResource(R.string.intake_title_collector_not_found),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colors.error,
            )
        }
        Text(
            text = stringResource(R.string.intake_body_collector_not_found),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colors.textPrimary.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun CollectorDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraExtraSmall)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colors.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colors.textPrimary,
        )
    }
}
