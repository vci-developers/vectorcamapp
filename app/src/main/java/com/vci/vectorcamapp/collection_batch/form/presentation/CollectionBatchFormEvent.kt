package com.vci.vectorcamapp.collection_batch.form.presentation

sealed interface CollectionBatchFormEvent {
    data object NavigateBackToCollectionBatchListScreen : CollectionBatchFormEvent
}
