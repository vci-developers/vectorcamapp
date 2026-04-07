package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.composites.FormAnswerAndQuestion
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import java.util.UUID

interface FormAnswerRepository {
    suspend fun upsertFormAnswer(
        formAnswer: FormAnswer,
        sessionId: UUID,
        questionId: Int
    ): Result<Unit, RoomDbError>

    suspend fun getFormAnswersBySessionId(sessionId: UUID): Map<Int, FormAnswer>
    suspend fun getFormAnswersAndQuestionsBySessionId(sessionId: UUID): List<FormAnswerAndQuestion>
}
