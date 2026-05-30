package com.vci.vectorcamapp.collection_batch.list.presentation

import java.util.UUID

sealed interface CollectionBatchListAction {
    data object AddCollectionBatch : CollectionBatchListAction
    data class EditCollectionBatch(val sessionUnitId: UUID) : CollectionBatchListAction
    data object SubmitSession : CollectionBatchListAction
}
