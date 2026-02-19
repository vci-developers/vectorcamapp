package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Upsert
    suspend fun upsertProgram(program: ProgramEntity)

    @Query("SELECT * FROM program")
    fun observeAllPrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM program WHERE id = :id")
    suspend fun getProgramById(id: Int): ProgramEntity?
}
