package com.vci.vectorcamapp.collection_batch.form.presentation

import java.util.UUID

sealed interface CollectionBatchFormEvent {
    data object NavigateBackToCollectionBatchListScreen : CollectionBatchFormEvent
    data class NavigateToImagingScreen(val sessionUnitId: UUID) : CollectionBatchFormEvent
}
