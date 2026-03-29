package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.FormAnswerDao
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FormAnswerRepositoryImplementation @Inject constructor(
    private val formAnswerDao: FormAnswerDao
) : FormAnswerRepository {

    override suspend fun upsertFormAnswer(
        formAnswer: FormAnswer,
        sessionId: Int,
        formId: Int,
        questionId: Int
    ): Result<Unit, RoomDbError> {
        return try {
            formAnswerDao.upsertFormAnswer(formAnswer.toEntity(sessionId, formId, questionId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override fun observeAnswersBySessionId(sessionId: String): Flow<List<FormAnswer>> {
        return formAnswerDao.observeAnswersBySessionId(sessionId).map { entities -> entities.map { it.toDomain() } }
    }
}
