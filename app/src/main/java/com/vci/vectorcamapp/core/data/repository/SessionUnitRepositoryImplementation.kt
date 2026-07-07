package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.SessionUnitDao
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class SessionUnitRepositoryImplementation @Inject constructor(
    private val sessionUnitDao: SessionUnitDao
) : SessionUnitRepository {

    override suspend fun upsertSessionUnit(sessionUnit: SessionUnit, sessionId: UUID): Result<Unit, RoomDbError> {
        return try {
            sessionUnitDao.upsertSessionUnit(sessionUnit.toEntity(sessionId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SessionUnitRepository: upsertSessionUnit failed")
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun getSessionUnitById(sessionUnitId: UUID): SessionUnit? {
        return sessionUnitDao.getSessionUnitById(sessionUnitId)?.toDomain()
    }

    override suspend fun getSessionUnitsForSession(sessionId: UUID): List<SessionUnit> {
        return sessionUnitDao.getSessionUnitsForSession(sessionId).map { it.toDomain() }
    }

    override suspend fun countSessionUnitsForSession(sessionId: UUID): Int {
        return sessionUnitDao.countSessionUnitsForSession(sessionId)
    }

    override fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnit>> {
        return sessionUnitDao.observeSessionUnitsForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun countSpecimensForSessionUnit(sessionUnitId: UUID): Int {
        return sessionUnitDao.countSpecimensForSessionUnit(sessionUnitId)
    }

    override suspend fun getMaxSessionUnitOrderForSession(sessionId: UUID): Int {
        return sessionUnitDao.getMaxSessionUnitOrderForSession(sessionId)
    }
}
