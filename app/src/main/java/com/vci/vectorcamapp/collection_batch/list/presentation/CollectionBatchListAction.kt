package com.vci.vectorcamapp.collection_batch.list.presentation

import java.util.UUID

sealed interface CollectionBatchListAction {
    data object AddCollectionBatch : CollectionBatchListAction
    data class EditCollectionBatch(val sessionUnitId: UUID) : CollectionBatchListAction
    data object OpenSubmitDialog : CollectionBatchListAction
    data object DismissSubmitDialog : CollectionBatchListAction
    data object SaveSessionProgress : CollectionBatchListAction
    data object ConfirmSubmitSession : CollectionBatchListAction
    data class SelectPendingAction(val pendingAction: CollectionBatchListAction) : CollectionBatchListAction
    data object ClearPendingAction : CollectionBatchListAction
    data object ConfirmPendingAction : CollectionBatchListAction
    data object DismissFormObsoleteDialog : CollectionBatchListAction
    data object GoToSettingsFromFormObsolete : CollectionBatchListAction
}
