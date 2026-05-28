package com.vci.vectorcamapp.collection_batch.list.presentation

import java.util.UUID

data class CollectionBatchListState(
    val isLoading: Boolean = true,
    val sessionId: String = "",
    val units: List<CollectionBatchCardData> = emptyList(),
)

data class CollectionBatchCardData(
    val localId: UUID,
    val unitOrder: Int,
    val bucketName: String,
    val specimenCount: Int,
    val createdAt: Long,
    val canDelete: Boolean,
)
