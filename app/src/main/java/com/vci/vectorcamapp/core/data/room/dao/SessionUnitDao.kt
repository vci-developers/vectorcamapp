package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SessionUnitDao {

    @Upsert
    suspend fun upsertSessionUnit(sessionUnit: SessionUnitEntity): Long

    @Query("SELECT * FROM session_unit WHERE localId = :sessionUnitId")
    suspend fun getSessionUnitById(sessionUnitId: UUID): SessionUnitEntity?

    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnitEntity>>

    @Query("SELECT COUNT(*) FROM specimen WHERE sessionUnitId = :sessionUnitId")
    suspend fun countSpecimensForSessionUnit(sessionUnitId: UUID): Int

    @Query("SELECT IFNULL(MAX(unitOrder), 0) FROM session_unit WHERE sessionId = :sessionId")
    suspend fun getMaxSessionUnitOrderForSession(sessionId: UUID): Int
}
