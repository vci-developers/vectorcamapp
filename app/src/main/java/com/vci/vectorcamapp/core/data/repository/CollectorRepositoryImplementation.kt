package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.CollectorDao
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CollectorRepositoryImplementation @Inject constructor(
    private val collectorDao: CollectorDao
) : CollectorRepository {
    override suspend fun upsertCollector(collector: Collector): Result<Unit, RoomDbError> {
        return try {
            collectorDao.upsertCollector(collector.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun deleteCollector(collector: Collector): Boolean {
        return collectorDao.deleteCollector(collector.toEntity()) > 0
    }

    override fun observeAllCollectors(): Flow<List<Collector>> {
        return collectorDao.observeAllCollectors().map { collectorEntities ->
            collectorEntities.map { it.toDomain() }
        }
    }
}