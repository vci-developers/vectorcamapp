package com.vci.vectorcamapp.core.domain.repository

import android.net.Uri
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSurveillanceForm
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.composites.SessionWithSpecimens
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SessionRepository {
    suspend fun upsertSession(session: Session, siteId: Int): Result<Unit, RoomDbError>
    suspend fun getSessionById(sessionId: UUID): Session?
    suspend fun deleteSession(session: Session, siteId: Int): Boolean
    suspend fun markSessionAsComplete(sessionId: UUID): Boolean
    suspend fun getSessionWithSpecimensById(sessionId: UUID): SessionWithSpecimens?
    suspend fun getSessionAndSurveillanceFormById(sessionId: UUID): SessionAndSurveillanceForm?
    suspend fun getSessionAndSiteById(sessionId: UUID): SessionAndSite?
    suspend fun getImageUrisBySessionId(sessionId: UUID): List<Uri>
    fun observeIncompleteSessionsAndSites(): Flow<List<SessionAndSite>>
    fun observeCompleteSessionsAndSites(): Flow<List<SessionAndSite>>
    fun observeSessionWithSpecimens(sessionId: UUID): Flow<SessionWithSpecimens?>
    suspend fun getIncompleteSessionsAndSites(): List<SessionAndSite>
}
