package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.composites.FormWithFormAnswersAndQuestions
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface FormRepository {
    suspend fun upsertForm(form: Form, programId: Int): Result<Unit, RoomDbError>
    fun observeFormsByProgramId(programId: Int): Flow<List<Form>>
    suspend fun getFormById(id: Int): Form?
    suspend fun getFormByVersion(version: String): Form?
    suspend fun getFormsWithFormAnswersAndQuestionsBySessionId(sessionId: UUID): List<FormWithFormAnswersAndQuestions>
}
