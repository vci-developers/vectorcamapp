package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.vci.vectorcamapp.core.data.room.entities.ProgramEntity

@Dao
interface ProgramDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(programs: List<ProgramEntity>)
}