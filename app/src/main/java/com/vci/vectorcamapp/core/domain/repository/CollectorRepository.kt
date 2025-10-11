package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow

interface CollectorRepository {
    suspend fun upsertCollector(collector: Collector): Result<Unit, RoomDbError>
    suspend fun deleteCollector(collector: Collector): Boolean
    fun observeAllCollectors(): Flow<List<Collector>>
}
