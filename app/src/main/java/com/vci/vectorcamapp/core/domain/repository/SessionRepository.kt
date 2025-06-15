package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.composites.SessionWithSpecimens
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SessionRepository {
    suspend fun upsertSession(session: Session, siteId: Int): Result<Unit, RoomDbError>
    suspend fun deleteSession(session: Session, siteId: Int): Boolean
    suspend fun markSessionAsComplete(sessionId: UUID): Boolean
    fun observeCompleteSessions(): Flow<List<Session>>
    fun observeIncompleteSessions(): Flow<List<Session>>
    fun observeSessionWithSpecimens(sessionId: UUID): Flow<SessionWithSpecimens?>
}
