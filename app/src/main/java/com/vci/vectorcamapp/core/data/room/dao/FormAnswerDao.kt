package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import java.util.UUID

@Dao
interface FormAnswerDao {
    @Upsert
    suspend fun upsertFormAnswer(formAnswer: FormAnswerEntity)

    @Query("SELECT * FROM form_answer WHERE sessionId = :sessionId")
    suspend fun getFormAnswersBySessionId(sessionId: UUID): List<FormAnswerEntity>
}
