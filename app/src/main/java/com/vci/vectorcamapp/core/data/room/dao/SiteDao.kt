package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vci.vectorcamapp.core.data.room.entities.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(sites: List<SiteEntity>)

    @Query("SELECT * FROM site WHERE programId = :programId")
    fun observeAllSitesByProgramId(programId: Int): Flow<List<SiteEntity>>

    @Query("SELECT * FROM site WHERE id = :id")
    suspend fun getSiteById(id: Int): SiteEntity?
}
