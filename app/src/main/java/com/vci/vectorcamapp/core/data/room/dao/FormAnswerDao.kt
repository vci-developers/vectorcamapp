package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.FormAnswerAndQuestionRelation
import java.util.UUID

@Dao
interface FormAnswerDao {
    @Upsert
    suspend fun upsertFormAnswer(formAnswer: FormAnswerEntity)

    @Query("SELECT * FROM form_answer WHERE sessionId = :sessionId")
    suspend fun getFormAnswersBySessionId(sessionId: UUID): List<FormAnswerEntity>

    @Transaction
    @Query("SELECT * FROM form_answer WHERE sessionId = :sessionId")
    suspend fun getFormAnswersAndQuestionsBySessionId(sessionId: UUID): List<FormAnswerAndQuestionRelation>
}
