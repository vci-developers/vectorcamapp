package com.vci.vectorcamapp.imaging.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.imaging.presentation.ImagingAction
import com.vci.vectorcamapp.imaging.presentation.ImagingState
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
internal fun ImagingExitDialog(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(ImagingAction.DismissExitDialog) },
        title = { ExitDialogTitle(state = state, onAction = onAction) },
        text = { ExitDialogBody(state = state) },
        confirmButton = { ExitDialogConfirmButton(state = state, onAction = onAction) },
        dismissButton = { ExitDialogDismissButton(state = state, onAction = onAction) },
    )
}

@Composable
private fun ExitDialogTitle(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimensions.paddingSmall),
    ) {
        Text(
            text = exitDialogTitle(state),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onAction(ImagingAction.DismissExitDialog) },
            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = stringResource(R.string.imaging_content_description_close_dialog),
                tint = MaterialTheme.colors.icon,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeExtraLarge),
            )
        }
    }
}

@Composable
private fun exitDialogTitle(state: ImagingState): String {
    return if (state.pendingAction == null) {
        if (state.sessionUnitId != null) {
            stringResource(R.string.imaging_title_leave)
        } else {
            stringResource(R.string.imaging_title_exit)
        }
    } else {
        stringResource(R.string.imaging_title_confirm_action)
    }
}

@Composable
private fun ExitDialogBody(state: ImagingState) {
    val dialogText = exitDialogMessage(state)
    Column {
        if (dialogText.isNotEmpty()) {
            Text(
                text = dialogText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textSecondary,
            )
        }
        if (state.specimensWithImagesAndInferenceResults.isEmpty() &&
            state.pendingAction is ImagingAction.SubmitSession
        ) {
            Text(
                text = stringResource(R.string.imaging_body_warning_zero_specimens),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(top = MaterialTheme.dimensions.paddingMedium),
            )
        }
        if (state.sessionType == SessionType.PRACTICE && state.pendingAction is ImagingAction.SubmitSession) {
            Text(
                text = stringResource(R.string.imaging_body_warning_practice),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(top = MaterialTheme.dimensions.paddingMedium),
            )
        }
    }
}

@Composable
private fun exitDialogMessage(state: ImagingState): String {
    return when (state.pendingAction) {
        null -> if (state.sessionUnitId != null) {
            stringResource(R.string.imaging_body_return_prompt)
        } else {
            stringResource(R.string.imaging_body_save_or_submit_prompt)
        }
        is ImagingAction.SaveSessionProgress -> stringResource(R.string.imaging_body_confirm_save)
        is ImagingAction.SubmitSession -> stringResource(R.string.imaging_body_confirm_submit)
        is ImagingAction.ReturnToCollectionBatchList -> stringResource(R.string.imaging_body_confirm_return)
        else -> ""
    }
}

@Composable
private fun ExitDialogConfirmButton(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
) {
    if (state.pendingAction != null) {
        Button(
            onClick = { onAction(ImagingAction.ConfirmPendingAction) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colors.error),
        ) {
            Text(
                text = stringResource(R.string.imaging_action_exit_dialog_confirm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.buttonText,
            )
        }
        return
    }
    if (state.sessionUnitId != null) return
    OutlinedButton(
        onClick = { onAction(ImagingAction.SelectPendingAction(ImagingAction.SubmitSession)) },
        border = BorderStroke(
            MaterialTheme.dimensions.borderThicknessThick,
            MaterialTheme.colors.successConfirm,
        ),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_cloud_upload),
            contentDescription = stringResource(R.string.imaging_content_description_submit),
            tint = MaterialTheme.colors.successConfirm,
            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall),
        )
        Spacer(Modifier.size(MaterialTheme.dimensions.paddingSmall))
        Text(
            text = stringResource(R.string.imaging_action_exit_dialog_submit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colors.successConfirm,
        )
    }
}

@Composable
private fun ExitDialogDismissButton(
    state: ImagingState,
    onAction: (ImagingAction) -> Unit,
) {
    if (state.pendingAction != null) {
        TextButton(onClick = { onAction(ImagingAction.ClearPendingAction) }) {
            Text(
                stringResource(R.string.imaging_action_exit_dialog_back),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textPrimary,
            )
        }
        return
    }
    val saveTargetAction: ImagingAction = if (state.sessionUnitId != null) {
        ImagingAction.ReturnToCollectionBatchList
    } else {
        ImagingAction.SaveSessionProgress
    }
    val isReturn = state.sessionUnitId != null
    OutlinedButton(
        onClick = { onAction(ImagingAction.SelectPendingAction(saveTargetAction)) },
        border = BorderStroke(MaterialTheme.dimensions.borderThicknessThick, MaterialTheme.colors.info),
    ) {
        Icon(
            painter = painterResource(id = if (isReturn) R.drawable.ic_arrow_left else R.drawable.ic_save),
            contentDescription = stringResource(
                if (isReturn) {
                    R.string.imaging_content_description_return
                } else {
                    R.string.imaging_content_description_save
                },
            ),
            tint = MaterialTheme.colors.info,
            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall),
        )
        Spacer(Modifier.size(MaterialTheme.dimensions.paddingSmall))
        Text(
            text = stringResource(
                if (isReturn) {
                    R.string.imaging_action_exit_dialog_return
                } else {
                    R.string.imaging_action_exit_dialog_save
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colors.info,
        )
    }
}
