package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.FormQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FormQuestionDao {
    @Upsert
    suspend fun upsertFormQuestion(formQuestion: FormQuestionEntity)

    @Query("SELECT * FROM form_question WHERE formId = :formId ORDER BY `order` ASC")
    fun observeQuestionsByFormId(formId: Int): Flow<List<FormQuestionEntity>>
}
