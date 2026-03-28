package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.FormEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormDao {
    @Upsert
    suspend fun upsertForm(form: FormEntity)

    @Query("SELECT * FROM form WHERE programId = :programId")
    fun observeFormsByProgramId(programId: Int): Flow<List<FormEntity>>

    @Query("SELECT * FROM form WHERE id = :id")
    suspend fun getFormById(id: Int): FormEntity?
}
