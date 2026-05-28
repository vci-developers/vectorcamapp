package com.vci.vectorcamapp.collection_batch.list.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    modifier: Modifier = Modifier,
) {
    ScreenHeader(
        title = "Collection Batches",
        subtitle = "Tap a card to edit, the arrow to image, or + to add",
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Add batch",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable { onAction(CollectionBatchListAction.AddCollectionBatch) }
            )
        },
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_cloud_upload),
                contentDescription = "Upload session",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable { onAction(CollectionBatchListAction.UploadSession) }
            )
        },
        modifier = modifier,
    ) {
        if (state.units.isEmpty()) {
            item {
                Text(
                    text = "No collection batches yet.\nTap + to add one.",
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
            items = state.units,
            key = { it.localId },
        ) { cardData ->
            CollectionBatchCard(
                cardData = cardData,
                onArrowClick = { onAction(CollectionBatchListAction.OpenCollectionBatchImaging(cardData.localId)) },
                onClick = { onAction(CollectionBatchListAction.EditCollectionBatch(cardData.localId)) },
            )
        }
    }
}
