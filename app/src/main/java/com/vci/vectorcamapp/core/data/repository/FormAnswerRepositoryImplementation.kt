package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.FormAnswerDao
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import java.util.UUID
import javax.inject.Inject

class FormAnswerRepositoryImplementation @Inject constructor(
    private val formAnswerDao: FormAnswerDao
) : FormAnswerRepository {

    override suspend fun upsertFormAnswer(
        formAnswer: FormAnswer,
        sessionId: UUID,
        sessionUnitId: UUID?,
        questionId: Int
    ): Result<Unit, RoomDbError> {
        return try {
            formAnswerDao.upsertFormAnswer(
                formAnswer.toEntity(
                    sessionId,
                    sessionUnitId,
                    questionId
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override suspend fun getFormAnswersBySessionId(sessionId: UUID): Map<Int, FormAnswer> {
        return formAnswerDao.getFormAnswersBySessionId(sessionId)
            .associate { it.questionId to it.toDomain() }
    }

    override suspend fun getSessionUnitScopedFormAnswersBySessionId(sessionId: UUID): Map<UUID, Map<Int, FormAnswer>> {
        return formAnswerDao.getSessionUnitScopedFormAnswersBySessionId(sessionId)
            .mapNotNull { formAnswerEntity -> formAnswerEntity.sessionUnitId?.let { it to formAnswerEntity } }
            .groupBy(
                keySelector = { (sessionUnitId, _) -> sessionUnitId },
                valueTransform = { (_, formAnswerEntity) -> formAnswerEntity }
            )
            .mapValues { (_, formAnswerEntities) -> formAnswerEntities.associate { it.questionId to it.toDomain() } }
    }
}
