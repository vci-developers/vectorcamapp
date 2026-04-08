package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    @Upsert
    suspend fun upsertSite(site: SiteEntity)

    @Query("SELECT * FROM site WHERE programId = :programId")
    fun observeAllSitesByProgramId(programId: Int): Flow<List<SiteEntity>>

    @Query("SELECT * FROM site WHERE id = :id")
    suspend fun getSiteById(id: Int): SiteEntity?

    @Query("UPDATE site SET isActive = 0 WHERE programId = :programId")
    suspend fun setAllSitesInactiveForProgram(programId: Int)
}
