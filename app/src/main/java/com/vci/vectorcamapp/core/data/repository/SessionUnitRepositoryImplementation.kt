package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.SessionUnitDao
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.composites.SessionUnitWithFormAnswers
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import java.util.UUID
import javax.inject.Inject

class SessionUnitRepositoryImplementation @Inject constructor(
    private val sessionUnitDao: SessionUnitDao
) : SessionUnitRepository {
    override suspend fun upsertSessionUnit(
        sessionUnit: SessionUnit,
        sessionId: UUID
    ): Result<Unit, RoomDbError> {
        return try {
            sessionUnitDao.upsertSessionUnit(sessionUnit.toEntity(sessionId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun deleteSessionUnit(sessionUnit: SessionUnit, sessionId: UUID): Boolean {
        return sessionUnitDao.deleteSessionUnit(sessionUnit.toEntity(sessionId)) > 0
    }

    override suspend fun getSessionUnitById(sessionUnitId: UUID): SessionUnit? {
        return sessionUnitDao.getSessionUnitById(sessionUnitId)?.toDomain()
    }

    override suspend fun getSessionUnitWithFormAnswers(sessionUnitId: UUID): SessionUnitWithFormAnswers? {
        val relation = sessionUnitDao.getSessionUnitWithFormAnswers(sessionUnitId)
        return relation?.let {
            SessionUnitWithFormAnswers(
                sessionUnit = it.sessionUnitEntity.toDomain(),
                formAnswers = it.formAnswerEntities.map { formAnswerEntity -> formAnswerEntity.toDomain() }
            )
        }
    }
}