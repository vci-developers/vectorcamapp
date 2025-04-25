package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.BoundingBoxEntity

@Dao
interface BoundingBoxDao {

    @Upsert
    suspend fun upsertBoundingBox(boundingBoxEntity: BoundingBoxEntity): Long
}
