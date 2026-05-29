package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.composites.SessionUnitWithFormAnswers
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import java.util.UUID

interface SessionUnitRepository {
    suspend fun upsertSessionUnit(sessionUnit: SessionUnit, sessionId: UUID): Result<Unit, RoomDbError>
    suspend fun deleteSessionUnit(sessionUnit: SessionUnit, sessionId: UUID): Boolean
    suspend fun getSessionUnitById(sessionUnitId: UUID): SessionUnit?
    suspend fun getSessionUnitWithFormAnswers(sessionUnitId: UUID): SessionUnitWithFormAnswers?
}
