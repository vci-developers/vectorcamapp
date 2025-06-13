package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SessionEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionAndSurveillanceFormRelation
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionWithSpecimensRelation
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SessionDao {

    @Upsert
    suspend fun upsertSession(session: SessionEntity): Long

    @Delete
    suspend fun deleteSession(session: SessionEntity): Int

    @Query("UPDATE session SET completedAt = :timestamp WHERE localId = :sessionId")
    suspend fun markSessionAsComplete(sessionId: UUID, timestamp: Long): Int

    @Query("SELECT * FROM session WHERE submittedAt IS NOT NULL")
    fun observeCompleteSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE submittedAt IS NULL")
    fun observeIncompleteSessions(): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM session WHERE localId = :sessionId")
    fun observeSessionAndSurveillanceForm(sessionId: UUID): Flow<SessionAndSurveillanceFormRelation?>

    @Transaction
    @Query("SELECT * FROM session WHERE localId = :sessionId")
    fun observeSessionWithSpecimens(sessionId: UUID): Flow<SessionWithSpecimensRelation?>
}
