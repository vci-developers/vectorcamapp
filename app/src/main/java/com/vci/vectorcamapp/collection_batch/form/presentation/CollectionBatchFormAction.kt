package com.vci.vectorcamapp.collection_batch.form.presentation

import com.vci.vectorcamapp.core.domain.model.FormAnswerValue

sealed interface CollectionBatchFormAction {
    data object ReturnToCollectionBatchListScreen: CollectionBatchFormAction
    data class UpdateFormAnswer(val questionId: Int, val value: FormAnswerValue) : CollectionBatchFormAction
    data object SubmitSessionUnitForm : CollectionBatchFormAction
}
