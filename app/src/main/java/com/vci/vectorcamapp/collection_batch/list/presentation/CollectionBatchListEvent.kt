package com.vci.vectorcamapp.collection_batch.list.presentation

import java.util.UUID

sealed interface CollectionBatchListEvent {
    data object NavigateBackToLandingScreen : CollectionBatchListEvent
    data class NavigateToCollectionBatchForm(
        val sessionId: UUID,
        val sessionUnitId: UUID?
    ) : CollectionBatchListEvent
}
