package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.FormQuestionDao
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import javax.inject.Inject

class FormQuestionRepositoryImplementation @Inject constructor(
    private val formQuestionDao: FormQuestionDao
) : FormQuestionRepository {

    override suspend fun upsertFormQuestion(
        formQuestion: FormQuestion, formId: Int, parentId: Int?
    ): Result<Unit, RoomDbError> {
        return try {
            formQuestionDao.upsertFormQuestion(formQuestion.toEntity(formId, parentId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun getQuestionsByFormIdAndScope(
        formId: Int,
        answerScope: FormQuestionScope?
    ): List<FormQuestion> {
        return formQuestionDao.getQuestionsByFormIdAndScope(formId, answerScope)
            .map { it.toDomain() }
    }

    override suspend fun getFormIdByQuestionId(questionId: Int): Int? {
        return formQuestionDao.getFormIdByQuestionId(questionId)
    }
}
