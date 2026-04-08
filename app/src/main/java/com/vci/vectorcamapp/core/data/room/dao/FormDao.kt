package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.FormEntity
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

    @Query("SELECT DISTINCT form.* FROM form INNER JOIN form_question ON form.id = form_question.formId INNER JOIN form_answer ON form_question.id = form_answer.questionId WHERE form_answer.sessionId = :sessionId")
    suspend fun getFormsBySessionId(sessionId: UUID): List<FormEntity>
}
