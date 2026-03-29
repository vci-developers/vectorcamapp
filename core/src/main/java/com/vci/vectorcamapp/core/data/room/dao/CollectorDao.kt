package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.CollectorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectorDao {

    @Upsert
    suspend fun upsertCollector(collectorEntity: CollectorEntity): Long

    @Delete
    suspend fun deleteCollector(collectorEntity: CollectorEntity): Int

    @Query("SELECT * FROM collector")
    fun observeAllCollectors(): Flow<List<CollectorEntity>>
}
