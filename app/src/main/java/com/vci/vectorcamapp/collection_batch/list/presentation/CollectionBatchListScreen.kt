package com.vci.vectorcamapp.collection_batch.list.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
            title = "Collection Batches",
            subtitle = "Create and edit collection batches here",
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = "Submit Session",
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
                        text = "No collection batches yet.\nTap the + button below to add one.",
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
                contentDescription = "Add Collection Batch",
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
                            text = "End session?",
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
                                contentDescription = "Close dialog",
                                tint = MaterialTheme.colors.icon,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeExtraLarge)
                            )
                        }
                    }
                },
                text = {
                    Text(
                        text = "Would you like to save this session for later, or submit it now?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textSecondary
                    )
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = { onAction(CollectionBatchListAction.ConfirmSubmitSession) },
                        border = BorderStroke(
                            MaterialTheme.dimensions.borderThicknessThick,
                            MaterialTheme.colors.successConfirm
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cloud_upload),
                            contentDescription = "Submit Icon",
                            tint = MaterialTheme.colors.successConfirm,
                            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
                        )
                        Spacer(Modifier.size(MaterialTheme.dimensions.paddingSmall))
                        Text(
                            text = "Submit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.successConfirm
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { onAction(CollectionBatchListAction.SaveSessionProgress) },
                        border = BorderStroke(
                            MaterialTheme.dimensions.borderThicknessThick,
                            MaterialTheme.colors.info
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save),
                            contentDescription = "Save Icon",
                            tint = MaterialTheme.colors.info,
                            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
                        )
                        Spacer(Modifier.size(MaterialTheme.dimensions.paddingSmall))
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.info
                        )
                    }
                }
            )
        }
    }
}
