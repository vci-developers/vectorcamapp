package com.vci.vectorcamapp.imaging.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.imaging.presentation.ImagingAction
import com.vci.vectorcamapp.imaging.presentation.ImagingState
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
internal fun FurtherProcessingDialog(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { },
        title = { FurtherProcessingTitle(modifier = modifier) },
        text = { FurtherProcessingBody(state = state, onAction = onAction) },
        confirmButton = {
            Button(
                onClick = { onAction(ImagingAction.SaveImageToSession) },
                enabled = state.hasConfirmedPackaging,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colors.successConfirm,
                    disabledContainerColor = MaterialTheme.colors.textSecondary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.imaging_action_further_processing_continue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.buttonText,
                )
            }
        },
    )
}

@Composable
private fun FurtherProcessingTitle(modifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.ic_info),
            contentDescription = stringResource(R.string.imaging_content_description_info),
            tint = MaterialTheme.colors.icon,
            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge),
        )
        Spacer(modifier.size(MaterialTheme.dimensions.spacingMedium))
        Text(
            text = stringResource(R.string.imaging_title_further_processing),
            color = MaterialTheme.colors.icon,
        )
    }
}

@Composable
private fun FurtherProcessingBody(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.imaging_body_further_processing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.size(MaterialTheme.dimensions.paddingMedium))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = state.hasConfirmedPackaging,
                onCheckedChange = { onAction(ImagingAction.TogglePackagingConfirmation(it)) },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.successConfirm),
            )
            Text(
                text = stringResource(R.string.imaging_label_packaging_confirmation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textPrimary,
                modifier = Modifier.padding(start = MaterialTheme.dimensions.paddingSmall),
            )
        }
    }
}
