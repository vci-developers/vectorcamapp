package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SessionUnitRepository {
    suspend fun upsertSessionUnit(
        sessionUnit: SessionUnit,
        sessionId: UUID,
    ): Result<Unit, RoomDbError>
    suspend fun getSessionUnitById(sessionUnitId: UUID): SessionUnit?
    fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnit>>
    suspend fun countSpecimensForSessionUnit(sessionUnitId: UUID): Int
    suspend fun getMaxSessionUnitOrderForSession(sessionId: UUID): Int
}
