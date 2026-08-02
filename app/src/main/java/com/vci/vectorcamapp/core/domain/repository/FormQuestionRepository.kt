package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError

interface FormQuestionRepository {
    suspend fun upsertFormQuestion(
        formQuestion: FormQuestion,
        formId: Int,
        parentId: Int?
    ): Result<Unit, RoomDbError>

    suspend fun getQuestionsByFormIdAndScope(
        formId: Int,
        answerScope: FormQuestionScope?
    ): List<FormQuestion>

    suspend fun getFormIdByQuestionId(questionId: Int): Int?
}
