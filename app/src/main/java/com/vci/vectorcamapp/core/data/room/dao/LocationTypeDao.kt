package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.LocationTypeEntity

@Dao
interface LocationTypeDao {

    @Upsert
    suspend fun upsertLocationType(locationType: LocationTypeEntity): Long
}
