package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.SessionUnitWithFormAnswersRelation
import java.util.UUID

@Dao
interface SessionUnitDao {

    @Upsert
    suspend fun upsertSessionUnit(sessionUnit: SessionUnitEntity): Long

    @Delete
    suspend fun deleteSessionUnit(sessionUnit: SessionUnitEntity): Int

    @Query("SELECT * FROM session_unit WHERE localId = :sessionUnitId")
    suspend fun getSessionUnitById(sessionUnitId: UUID): SessionUnitEntity?

    @Transaction
    @Query("SELECT * FROM session_unit WHERE localId = :sessionUnitId ORDER BY unitOrder ASC")
    suspend fun getSessionUnitWithFormAnswers(sessionUnitId: UUID): SessionUnitWithFormAnswersRelation?
}
