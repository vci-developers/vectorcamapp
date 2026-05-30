package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.room.dao.SessionUnitDao
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class SessionUnitRepositoryImplementation @Inject constructor(
    private val sessionUnitDao: SessionUnitDao
) : SessionUnitRepository {

    override fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnit>> {
        return sessionUnitDao.observeSessionUnitsForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun countSpecimensForSessionUnit(sessionUnitId: UUID): Int {
        return sessionUnitDao.countSpecimensForSessionUnit(sessionUnitId)
    }
}
