package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SessionUnitRepository {
    suspend fun upsertSessionUnit(unit: SessionUnit): Result<Unit, RoomDbError>
    suspend fun getSessionUnitById(unitId: UUID): SessionUnit?
    suspend fun getSessionUnitsForSession(sessionId: UUID): List<SessionUnit>
    fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnit>>
    suspend fun getNextUnitOrder(sessionId: UUID): Int
    suspend fun countSessionUnits(sessionId: UUID): Int
    suspend fun countSpecimensForUnit(unitId: UUID): Int

    /** Deletes the unit only if it has no specimens. Returns true on delete, false if guarded. */
    suspend fun deleteSessionUnitIfNoSpecimens(unit: SessionUnit): Boolean
}
