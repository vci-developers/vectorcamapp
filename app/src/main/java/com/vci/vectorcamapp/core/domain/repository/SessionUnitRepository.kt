package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.SessionUnit
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SessionUnitRepository {
    fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnit>>
    suspend fun countSpecimensForSessionUnit(sessionUnitId: UUID): Int
}
