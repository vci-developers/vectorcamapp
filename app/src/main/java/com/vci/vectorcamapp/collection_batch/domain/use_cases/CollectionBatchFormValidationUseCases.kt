package com.vci.vectorcamapp.collection_batch.domain.use_cases

import javax.inject.Inject

data class CollectionBatchFormValidationUseCases @Inject constructor(
    val validateFormAnswers: ValidateFormAnswersUseCase,
    val validateCollectionBatchIdentity: ValidateCollectionBatchIdentityUseCase,
)
