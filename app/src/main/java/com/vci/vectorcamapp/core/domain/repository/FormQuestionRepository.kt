package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow

interface FormQuestionRepository {
    suspend fun upsertFormQuestion(
        formQuestion: FormQuestion,
        formId: Int,
        parentId: Int?
    ): Result<Unit, RoomDbError>

    fun observeQuestionsByFormId(formId: Int): Flow<List<FormQuestion>>
}
