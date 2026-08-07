package com.vci.vectorcamapp.collection_batch.list.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.collection_batch.list.presentation.components.CollectionBatchCard
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun CollectionBatchListScreen(
    state: CollectionBatchListState,
    onAction: (CollectionBatchListAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.collection_batch_title_screen),
            subtitle = stringResource(R.string.collection_batch_body_subtitle),
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = stringResource(R.string.collection_batch_content_description_submit_session),
                    tint = MaterialTheme.colors.icon,
                    modifier = Modifier
                        .size(MaterialTheme.dimensions.iconSizeLarge)
                        .clickable { onAction(CollectionBatchListAction.OpenSubmitDialog) }
                )
            },
        ) {
            if (state.sessionUnits.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.collection_batch_body_empty),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.dimensions.paddingMedium)
                    )
                }
            }

            items(
                items = state.sessionUnits,
                key = { it.localId }
            ) { sessionUnit ->
                CollectionBatchCard(
                    bucketName = state.bucketNamesBySessionUnitId[sessionUnit.localId].orEmpty(),
                    sessionUnit = sessionUnit,
                    specimenCount = state.specimenCountsBySessionUnitId[sessionUnit.localId] ?: 0,
                    onClick = { onAction(CollectionBatchListAction.EditCollectionBatch(sessionUnit.localId)) }
                )
            }
        }

        FloatingActionButton(
            onClick = { onAction(CollectionBatchListAction.AddCollectionBatch) },
            containerColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.buttonText,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MaterialTheme.dimensions.paddingLarge)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.collection_batch_content_description_add),
                tint = MaterialTheme.colors.buttonText,
                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
            )
        }

        if (state.isSubmitDialogVisible) {
            AlertDialog(
                onDismissRequest = { onAction(CollectionBatchListAction.DismissSubmitDialog) },
                title = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.submissionPendingAction == null) stringResource(R.string.collection_batch_title_end_session) else stringResource(R.string.collection_batch_title_confirm_action),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onAction(CollectionBatchListAction.DismissSubmitDialog) },
                            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.collection_batch_content_description_close_dialog),
                                tint = MaterialTheme.colors.icon,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeExtraLarge)
                            )
                        }
                    }
                },
                text = {
                    val dialogText = when (state.submissionPendingAction) {
                        null -> stringResource(R.string.collection_batch_body_save_or_submit)
                        is CollectionBatchListAction.SaveSessionProgress -> stringResource(R.string.collection_batch_body_confirm_save)
                        is CollectionBatchListAction.ConfirmSubmitSession -> stringResource(R.string.collection_batch_body_confirm_submit)
                        else -> ""
                    }
                    Column {
                        if (dialogText.isNotEmpty()) {
                            Text(
                                text = dialogText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.textSecondary
                            )
                        }

                        if (state.sessionUnits.isEmpty() && state.submissionPendingAction is CollectionBatchListAction.ConfirmSubmitSession) {
                            Text(
                                text = stringResource(R.string.collection_batch_body_warning_zero),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colors.error,
                                modifier = Modifier.padding(top = MaterialTheme.dimensions.paddingMedium)
                            )
                        }
                    }
                },
                confirmButton = {
                    if (state.submissionPendingAction == null) {
                        OutlinedButton(
                            onClick = {
                                onAction(
                                    CollectionBatchListAction.SelectPendingAction(
                                        CollectionBatchListAction.ConfirmSubmitSession
                                    )
                                )
                            },
                            border = BorderStroke(
                                MaterialTheme.dimensions.borderThicknessThick,
                                MaterialTheme.colors.successConfirm
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cloud_upload),
                                contentDescription = stringResource(R.string.collection_batch_content_description_submit_icon),
                                tint = MaterialTheme.colors.successConfirm,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
                            )
                            Spacer(Modifier.size(MaterialTheme.dimensions.paddingSmall))
                            Text(
                                text = stringResource(R.string.collection_batch_action_exit_dialog_submit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.successConfirm
                            )
                        }
                    } else {
                        Button(
                            onClick = { onAction(CollectionBatchListAction.ConfirmPendingAction) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colors.error
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.collection_batch_action_exit_dialog_confirm),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.buttonText
                            )
                        }
                    }
                },
                dismissButton = {
                    if (state.submissionPendingAction == null) {
                        OutlinedButton(
                            onClick = {
                                onAction(
                                    CollectionBatchListAction.SelectPendingAction(
                                        CollectionBatchListAction.SaveSessionProgress
                                    )
                                )
                            },
                            border = BorderStroke(
                                MaterialTheme.dimensions.borderThicknessThick,
                                MaterialTheme.colors.info
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_save),
                                contentDescription = stringResource(R.string.collection_batch_content_description_save_icon),
                                tint = MaterialTheme.colors.info,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
                            )
                            Spacer(Modifier.size(MaterialTheme.dimensions.paddingSmall))
                            Text(
                                text = stringResource(R.string.collection_batch_action_exit_dialog_save),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.info
                            )
                        }
                    } else {
                        TextButton(onClick = { onAction(CollectionBatchListAction.ClearPendingAction) }) {
                            Text(
                                stringResource(R.string.collection_batch_action_exit_dialog_back),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.textPrimary
                            )
                        }
                    }
                }
            )
        }

        if (state.showFormObsoleteDialog) {
            AlertDialog(
                onDismissRequest = { onAction(CollectionBatchListAction.DismissFormObsoleteDialog) },
                title = {
                    Text(
                        text = "Form Update Required",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "The form version on your device is out of date. Please sync to get the latest form before starting a new session.\n\nWould you like to go to Settings to sync now?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onAction(CollectionBatchListAction.GoToSettingsFromFormObsolete) }
                    ) {
                        Text(
                            text = "Go to Settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.buttonText
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onAction(CollectionBatchListAction.DismissFormObsoleteDialog) }
                    ) {
                        Text(
                            text = "Later",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
                }
            )
        }
    }
}
