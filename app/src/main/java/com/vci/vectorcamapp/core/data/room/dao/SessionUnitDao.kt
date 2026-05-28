package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionUnitWithAnswersRelation
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionUnitWithSpecimensRelation
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SessionUnitDao {

    @Upsert
    suspend fun upsertSessionUnit(unit: SessionUnitEntity): Long

    @Delete
    suspend fun deleteSessionUnit(unit: SessionUnitEntity): Int

    @Query("SELECT * FROM session_unit WHERE localId = :unitId")
    suspend fun getSessionUnitById(unitId: UUID): SessionUnitEntity?

    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    suspend fun getSessionUnitsForSession(sessionId: UUID): List<SessionUnitEntity>

    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnitEntity>>

    @Query("SELECT COALESCE(MAX(unitOrder), 0) FROM session_unit WHERE sessionId = :sessionId")
    suspend fun getMaxUnitOrderForSession(sessionId: UUID): Int

    @Query("SELECT COUNT(*) FROM session_unit WHERE sessionId = :sessionId")
    suspend fun countSessionUnitsForSession(sessionId: UUID): Int

    @Query("SELECT COUNT(*) FROM specimen WHERE sessionUnitId = :unitId")
    suspend fun countSpecimensForUnit(unitId: UUID): Int

    @Transaction
    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    suspend fun getSessionUnitsWithAnswersForSession(sessionId: UUID): List<SessionUnitWithAnswersRelation>

    @Transaction
    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    fun observeSessionUnitsWithAnswersForSession(sessionId: UUID): Flow<List<SessionUnitWithAnswersRelation>>

    @Transaction
    @Query("SELECT * FROM session_unit WHERE localId = :unitId")
    suspend fun getSessionUnitWithAnswers(unitId: UUID): SessionUnitWithAnswersRelation?
}
