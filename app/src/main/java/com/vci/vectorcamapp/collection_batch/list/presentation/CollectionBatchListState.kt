package com.vci.vectorcamapp.collection_batch.list.presentation

import com.vci.vectorcamapp.core.domain.model.SessionUnit
import java.util.UUID

data class CollectionBatchListState(
    val isLoading: Boolean = true,
    val sessionId: UUID = UUID(0, 0),
    val sessionUnits: List<SessionUnit> = emptyList(),
    val specimenCountsBySessionUnitId: Map<UUID, Int> = emptyMap(),
    val bucketNamesBySessionUnitId: Map<UUID, String> = emptyMap(),
    val isSubmitDialogVisible: Boolean = false,
    val submissionPendingAction: CollectionBatchListAction? = null,
)
