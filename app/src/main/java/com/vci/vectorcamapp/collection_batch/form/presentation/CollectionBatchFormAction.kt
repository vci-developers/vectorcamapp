package com.vci.vectorcamapp.collection_batch.form.presentation

sealed interface CollectionBatchFormAction {
    data object ReturnToPreviousScreen : CollectionBatchFormAction
    data class EnterAnswer(val questionId: Int, val value: String) : CollectionBatchFormAction
    data object Confirm : CollectionBatchFormAction
}
