package com.vci.vectorcamapp.core.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vci.vectorcamapp.core.data.room.entities.FormQuestionEntity
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope

@Dao
interface FormQuestionDao {
    @Upsert
    suspend fun upsertFormQuestion(formQuestion: FormQuestionEntity)

    @Query(
        """
            SELECT * FROM form_question
            WHERE formId = :formId
              AND (:answerScope IS NULL OR answerScope = :answerScope)
            ORDER BY `order` ASC
        """
    )
    suspend fun getQuestionsByFormIdAndScope(
        formId: Int,
        answerScope: FormQuestionScope?
    ): List<FormQuestionEntity>
}
