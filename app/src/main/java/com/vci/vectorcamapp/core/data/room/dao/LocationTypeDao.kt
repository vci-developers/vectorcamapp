package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.LocationTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationTypeDao {

    @Upsert
    suspend fun upsertLocationType(locationType: LocationTypeEntity): Long

    @Query("SELECT * FROM location_type WHERE programId = :programId ORDER BY level ASC")
    fun observeAllLocationTypesByProgramId(programId: Int): Flow<List<LocationTypeEntity>>
}
