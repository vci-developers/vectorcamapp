package com.vci.vectorcamapp.collection_batch.list.presentation

import java.util.UUID

sealed interface CollectionBatchListAction {
    data object ReturnToPreviousScreen : CollectionBatchListAction
    data object AddCollectionBatch : CollectionBatchListAction
    data class OpenCollectionBatchImaging(val unitId: UUID) : CollectionBatchListAction
    data class EditCollectionBatch(val unitId: UUID) : CollectionBatchListAction
    data class DeleteCollectionBatch(val unitId: UUID) : CollectionBatchListAction
    data object UploadSession : CollectionBatchListAction
}
