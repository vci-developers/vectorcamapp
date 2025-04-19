package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SpecimenEntity

@Dao
interface SpecimenDao {

    @Upsert
    suspend fun upsertSpecimen(specimen: SpecimenEntity): Long

    @Delete
    suspend fun deleteSpecimen(specimen: SpecimenEntity): Int
}
