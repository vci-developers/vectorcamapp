package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SessionUnitDao {

    @Query("SELECT * FROM session_unit WHERE sessionId = :sessionId ORDER BY unitOrder ASC")
    fun observeSessionUnitsForSession(sessionId: UUID): Flow<List<SessionUnitEntity>>

    @Query("SELECT COUNT(*) FROM specimen WHERE sessionUnitId = :sessionUnitId")
    suspend fun countSpecimensForSessionUnit(sessionUnitId: UUID): Int
}
