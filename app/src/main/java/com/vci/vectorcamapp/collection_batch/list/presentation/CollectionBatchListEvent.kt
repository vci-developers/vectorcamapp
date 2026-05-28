package com.vci.vectorcamapp.collection_batch.list.presentation

sealed interface CollectionBatchListEvent {
    data object NavigateBackToLandingScreen : CollectionBatchListEvent
    data class NavigateToCollectionBatchForm(val sessionId: String, val unitId: String?) : CollectionBatchListEvent
    data class NavigateToImaging(val sessionUnitId: String) : CollectionBatchListEvent
}
