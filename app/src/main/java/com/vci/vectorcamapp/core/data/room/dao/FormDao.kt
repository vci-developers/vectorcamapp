package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.FormEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.FormAnswerAndQuestionRelation
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface FormDao {
    @Upsert
    suspend fun upsertForm(form: FormEntity)

    @Query("SELECT * FROM form WHERE programId = :programId")
    fun observeFormsByProgramId(programId: Int): Flow<List<FormEntity>>

    @Query("SELECT * FROM form WHERE id = :id")
    suspend fun getFormById(id: Int): FormEntity?

    @Query("SELECT * FROM form WHERE version = :version")
    suspend fun getFormByVersion(version: String): FormEntity?

    @Transaction
    @Query("SELECT * FROM form_answer WHERE sessionId = :sessionId")
    suspend fun getFormAnswersAndQuestionsBySessionId(sessionId: UUID): List<FormAnswerAndQuestionRelation>
}

