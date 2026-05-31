package com.vci.vectorcamapp.collection_batch.form.presentation

sealed interface CollectionBatchFormAction {
    data object ReturnToCollectionBatchListScreen: CollectionBatchFormAction
    data class UpdateFormAnswer(val questionId: Int, val value: String) : CollectionBatchFormAction
}