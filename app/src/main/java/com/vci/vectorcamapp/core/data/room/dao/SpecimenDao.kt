package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.SpecimenEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.SpecimenAndBoundingBoxRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecimenDao {

    @Upsert
    suspend fun upsertSpecimen(specimen: SpecimenEntity): Long

    @Delete
    suspend fun deleteSpecimen(specimen: SpecimenEntity): Int

    @Transaction
    @Query("SELECT * FROM specimen WHERE id = :specimenId")
    fun observeSpecimenAndBoundingBox(specimenId: String) : Flow<SpecimenAndBoundingBoxRelation?>
}
