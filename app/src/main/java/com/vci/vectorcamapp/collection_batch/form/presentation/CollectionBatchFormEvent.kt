package com.vci.vectorcamapp.collection_batch.form.presentation

sealed interface CollectionBatchFormEvent {
    data object NavigateBackToPreviousScreen : CollectionBatchFormEvent
    data class NavigateToImagingScreen(val sessionUnitId: String) : CollectionBatchFormEvent
}
