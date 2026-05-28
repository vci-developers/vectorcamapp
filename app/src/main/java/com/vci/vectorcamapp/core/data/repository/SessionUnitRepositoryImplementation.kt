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
import java.util.UUID
import javax.inject.Inject

class SessionUnitRepositoryImplementation @Inject constructor(
    private val sessionUnitDao: SessionUnitDao
) : SessionUnitRepository {

    override suspend fun upsertSessionUnit(unit: SessionUnit): Result<Unit, RoomDbError> {
        return try {
            sessionUnitDao.upsertSessionUnit(unit.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun getSessionUnitById(unitId: UUID): SessionUnit? {
        return sessionUnitDao.getSessionUnitById(unitId)?.toDomain()
    }

    override suspend fun getSessionUnitsForSession(sessionId: UUID): List<SessionUnit> {
        return sessionUnitDao.getSessionUnitsForSession(sessionId).map { it.toDomain() }
    }

    override fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnit>> {
        return sessionUnitDao.observeSessionUnitsForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNextUnitOrder(sessionId: UUID): Int {
        return sessionUnitDao.getMaxUnitOrderForSession(sessionId) + 1
    }

    override suspend fun countSessionUnits(sessionId: UUID): Int {
        return sessionUnitDao.countSessionUnitsForSession(sessionId)
    }

    override suspend fun countSpecimensForUnit(unitId: UUID): Int {
        return sessionUnitDao.countSpecimensForUnit(unitId)
    }

    override suspend fun deleteSessionUnitIfNoSpecimens(unit: SessionUnit): Boolean {
        val specimenCount = sessionUnitDao.countSpecimensForUnit(unit.localId)
        if (specimenCount > 0) return false
        sessionUnitDao.deleteSessionUnit(unit.toEntity())
        return true
    }
}
