package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.composites.SessionWithSpecimens
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SessionRepository {
    suspend fun upsertSession(session: Session): Boolean
    suspend fun deleteSession(session: Session): Boolean
    suspend fun markSessionAsComplete(sessionId: UUID): Boolean
    fun observeCompleteSessions(): Flow<List<Session>>
    fun observeIncompleteSessions(): Flow<List<Session>>
    fun observeSessionWithSpecimens(sessionId: UUID): Flow<SessionWithSpecimens?>
}
