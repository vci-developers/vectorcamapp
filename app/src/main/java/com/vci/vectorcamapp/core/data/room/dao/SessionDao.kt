package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SessionEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionAndSurveillanceFormRelation
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionAndSiteRelation
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionWithSpecimensRelation
import com.vci.vectorcamapp.core.data.room.helpers.SessionCounts
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SessionDao {

    @Upsert
    suspend fun upsertSession(session: SessionEntity): Long

    @Query("SELECT * FROM session WHERE localId = :sessionId")
    suspend fun getSessionById(sessionId: UUID): SessionEntity?

    @Delete
    suspend fun deleteSession(session: SessionEntity): Int

    @Query("UPDATE session SET completedAt = :timestamp WHERE localId = :sessionId")
    suspend fun markSessionAsComplete(sessionId: UUID, timestamp: Long): Int

    @Transaction
    @Query("SELECT * FROM session WHERE localId = :sessionId")
    suspend fun getSessionWithSpecimens(sessionId: UUID): SessionWithSpecimensRelation?

    @Transaction
    @Query("SELECT * FROM session WHERE localId = :sessionId")
    suspend fun getSessionAndSurveillanceForm(sessionId: UUID): SessionAndSurveillanceFormRelation?

    @Transaction
    @Query("SELECT * FROM session WHERE localId = :sessionId")
    suspend fun getSessionAndSiteById(sessionId: UUID): SessionAndSiteRelation?

    @Query("SELECT * FROM session WHERE completedAt IS NULL")
    fun observeIncompleteSessions(): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM session WHERE completedAt IS NOT NULL")
    fun observeCompleteSessionsAndSites(): Flow<List<SessionAndSiteRelation>>

    @Transaction
    @Query("SELECT * FROM session WHERE localId = :sessionId")
    fun observeSessionWithSpecimens(sessionId: UUID): Flow<SessionWithSpecimensRelation?>

    @Query("""
        SELECT s.localId as sessionId,
               COALESCE(uc.uploaded, 0) as uploadedImages,
               COALESCE(uc.total, 0) as totalImages
        FROM session s
        LEFT JOIN (
            SELECT si.sessionId,
                   COUNT(si.localId) as total,
                   COUNT(CASE WHEN si.metadataUploadStatus = 'COMPLETED' 
                              AND si.imageUploadStatus = 'COMPLETED' THEN 1 END) as uploaded
            FROM specimen_image si
            GROUP BY si.sessionId
        ) uc ON s.localId = uc.sessionId
        WHERE s.localId IN (:sessionIds)
    """)
    fun observeSessionCounts(sessionIds: List<UUID>): Flow<List<SessionCounts>>
}
