package com.vci.vectorcamapp.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.dao.FormAnswerDao
import com.vci.vectorcamapp.core.data.room.dao.FormDao
import com.vci.vectorcamapp.core.data.room.dao.FormQuestionDao
import com.vci.vectorcamapp.core.data.room.dao.SessionUnitDao
import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.data.room.entities.FormEntity
import com.vci.vectorcamapp.core.data.room.entities.FormQuestionEntity
import com.vci.vectorcamapp.core.data.room.entities.SessionUnitEntity
import com.vci.vectorcamapp.core.data.room.entities.relations.FormAnswerAndQuestionRelation
import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class FormRepositoryImplementationTest {

    private val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000041")
    private val unitId = UUID.fromString("00000000-0000-0000-0000-000000000042")
    private val answerId = UUID.fromString("00000000-0000-0000-0000-000000000043")

    @Test
    fun form_upsertAndLookup() = runTest {
        val dao = mockk<FormDao>()
        val repository = FormRepositoryImplementation(dao)
        val form = Form(3, "Intake", "v1")
        coEvery { dao.upsertForm(any()) } returns Unit
        coEvery { dao.getFormById(3) } returns FormEntity(3, 7, "Intake", "v1")
        coEvery { dao.getFormByVersion("missing") } returns null

        assertThat(repository.upsertForm(form, 7)).isEqualTo(Result.Success(Unit))
        assertThat(repository.getFormById(3)).isEqualTo(form)
        assertThat(repository.getFormByVersion("missing")).isNull()
    }

    @Test
    fun form_upsertFailure() = runTest {
        val dao = mockk<FormDao>()
        val repository = FormRepositoryImplementation(dao)
        coEvery { dao.upsertForm(any()) } throws IllegalStateException("db")

        assertThat(repository.upsertForm(Form(1, "F", "v"), 1))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
    }

    @Test
    fun form_groupsAnswersByFormAndSkipsMissingForms() = runTest {
        val dao = mockk<FormDao>()
        val repository = FormRepositoryImplementation(dao)
        val kept = relation(formId = 3, questionId = 2, order = 2, value = "later")
        val earlier = relation(formId = 3, questionId = 1, order = 1, value = "first")
        val orphan = relation(formId = 9, questionId = 4, order = 1, value = "gone")
        coEvery { dao.getFormAnswersAndQuestionsBySessionId(sessionId) } returns listOf(kept, earlier, orphan)
        coEvery { dao.getFormById(3) } returns FormEntity(3, 7, "Intake", "v1")
        coEvery { dao.getFormById(9) } returns null

        val grouped = repository.getFormsWithFormAnswersAndQuestionsBySessionId(sessionId)

        assertThat(grouped).hasSize(1)
        assertThat(grouped.single().form).isEqualTo(Form(3, "Intake", "v1"))
        assertThat(grouped.single().formAnswersAndQuestions.map { it.answer.value })
            .containsExactly("first", "later")
            .inOrder()
    }

    @Test
    fun questions_upsertLookupAndFormId() = runTest {
        val dao = mockk<FormQuestionDao>()
        val repository = FormQuestionRepositoryImplementation(dao)
        val question = sampleQuestion()
        coEvery { dao.upsertFormQuestion(any()) } returns Unit
        coEvery { dao.getQuestionsByFormIdAndScope(3, FormQuestionScope.SESSION_UNIT) } returns listOf(
            FormQuestionEntity(
                id = 1,
                formId = 3,
                label = "Village",
                type = "text",
                answerScope = FormQuestionScope.SESSION_UNIT,
                isUnitIdentityComponent = true,
            )
        )
        coEvery { dao.getFormIdByQuestionId(1) } returns 3
        coEvery { dao.getFormIdByQuestionId(99) } returns null

        assertThat(repository.upsertFormQuestion(question, formId = 3, parentId = null))
            .isEqualTo(Result.Success(Unit))
        assertThat(repository.getQuestionsByFormIdAndScope(3, FormQuestionScope.SESSION_UNIT))
            .containsExactly(question)
        assertThat(repository.getFormIdByQuestionId(1)).isEqualTo(3)
        assertThat(repository.getFormIdByQuestionId(99)).isNull()
    }

    @Test
    fun questions_upsertFailure() = runTest {
        val dao = mockk<FormQuestionDao>()
        val repository = FormQuestionRepositoryImplementation(dao)
        coEvery { dao.upsertFormQuestion(any()) } throws IllegalStateException("db")

        assertThat(repository.upsertFormQuestion(sampleQuestion(), 3, 8))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
    }

    @Test
    fun answers_roundTripBySessionAndUnit() = runTest {
        val dao = mockk<FormAnswerDao>()
        val repository = FormAnswerRepositoryImplementation(dao)
        val answer = sampleAnswer()
        coEvery { dao.upsertFormAnswer(any()) } returns Unit
        coEvery { dao.getSessionScopedFormAnswers(sessionId) } returns listOf(sampleAnswerEntity(sessionUnitId = null))
        coEvery { dao.getSessionUnitScopedFormAnswers(unitId) } returns listOf(sampleAnswerEntity(sessionUnitId = unitId))

        assertThat(repository.upsertFormAnswer(answer, sessionId, unitId, 1)).isEqualTo(Result.Success(Unit))
        assertThat(repository.getSessionScopedFormAnswers(sessionId)).containsEntry(1, answer)
        assertThat(repository.getSessionUnitScopedFormAnswers(unitId)).containsEntry(1, answer)
        coVerify { dao.upsertFormAnswer(match { it.sessionUnitId == unitId && it.questionId == 1 }) }
    }

    @Test
    fun answers_observeGroupsByUnitAndDropsSessionScopedRows() = runTest {
        val dao = mockk<FormAnswerDao>()
        val repository = FormAnswerRepositoryImplementation(dao)
        every { dao.observeSessionUnitScopedFormAnswersBySessionId(sessionId) } returns flowOf(
            listOf(
                sampleAnswerEntity(sessionUnitId = unitId),
                sampleAnswerEntity(sessionUnitId = null, questionId = 2),
            )
        )

        repository.observeSessionUnitScopedFormAnswersBySessionId(sessionId).test {
            val grouped = awaitItem()
            assertThat(grouped.keys).containsExactly(unitId)
            assertThat(grouped.getValue(unitId).keys).containsExactly(1)
            awaitComplete()
        }
    }

    @Test
    fun answers_upsertFailure() = runTest {
        val dao = mockk<FormAnswerDao>()
        val repository = FormAnswerRepositoryImplementation(dao)
        coEvery { dao.upsertFormAnswer(any()) } throws IllegalStateException("db")

        assertThat(repository.upsertFormAnswer(sampleAnswer(), sessionId, null, 1))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
    }

    @Test
    fun sessionUnits_coverEachQuery() = runTest {
        val dao = mockk<SessionUnitDao>()
        val repository = SessionUnitRepositoryImplementation(dao)
        val unit = SessionUnit(unitId, remoteId = 4, unitOrder = 2, createdAt = 8L)
        val entity = SessionUnitEntity(unitId, sessionId, remoteId = 4, unitOrder = 2, createdAt = 8L)
        coEvery { dao.upsertSessionUnit(any()) } returns 1L
        coEvery { dao.getSessionUnitById(unitId) } returns entity
        coEvery { dao.getSessionUnitsForSession(sessionId) } returns listOf(entity)
        coEvery { dao.countSessionUnitsForSession(sessionId) } returns 1
        coEvery { dao.countSpecimensForSessionUnit(unitId) } returns 5
        coEvery { dao.getMaxSessionUnitOrderForSession(sessionId) } returns 2
        every { dao.observeSessionUnitsForSession(sessionId) } returns flowOf(listOf(entity))

        assertThat(repository.upsertSessionUnit(unit, sessionId)).isEqualTo(Result.Success(Unit))
        assertThat(repository.getSessionUnitById(unitId)).isEqualTo(unit)
        assertThat(repository.getSessionUnitsForSession(sessionId)).containsExactly(unit)
        assertThat(repository.countSessionUnitsForSession(sessionId)).isEqualTo(1)
        assertThat(repository.countSpecimensForSessionUnit(unitId)).isEqualTo(5)
        assertThat(repository.getMaxSessionUnitOrderForSession(sessionId)).isEqualTo(2)
        repository.observeSessionUnitsForSession(sessionId).test {
            assertThat(awaitItem()).containsExactly(unit)
            awaitComplete()
        }
    }

    @Test
    fun sessionUnits_missingAndFailedUpsert() = runTest {
        val dao = mockk<SessionUnitDao>()
        val repository = SessionUnitRepositoryImplementation(dao)
        coEvery { dao.upsertSessionUnit(any()) } throws IllegalStateException("db")
        coEvery { dao.getSessionUnitById(unitId) } returns null

        assertThat(repository.upsertSessionUnit(SessionUnit(unitId, null, 1, 1L), sessionId))
            .isEqualTo(Result.Error(RoomDbError.UNKNOWN_ERROR))
        assertThat(repository.getSessionUnitById(unitId)).isNull()
    }

    private fun sampleQuestion() = FormQuestion(
        id = 1,
        label = "Village",
        type = "text",
        required = false,
        prerequisite = null,
        options = null,
        order = null,
        answerScope = FormQuestionScope.SESSION_UNIT,
        isUnitIdentityComponent = true,
    )

    private fun sampleAnswer() = FormAnswer(
        localId = answerId,
        remoteId = null,
        value = "Kampala",
        dataType = "text",
        submittedAt = 3L,
    )

    private fun sampleAnswerEntity(sessionUnitId: UUID?, questionId: Int = 1) = FormAnswerEntity(
        localId = answerId,
        remoteId = null,
        sessionId = sessionId,
        sessionUnitId = sessionUnitId,
        questionId = questionId,
        value = "Kampala",
        dataType = "text",
        submittedAt = 3L,
    )

    private fun relation(formId: Int, questionId: Int, order: Int, value: String) = FormAnswerAndQuestionRelation(
        answer = FormAnswerEntity(
            localId = UUID.randomUUID(),
            sessionId = sessionId,
            questionId = questionId,
            value = value,
            dataType = "text",
        ),
        question = FormQuestionEntity(
            id = questionId,
            formId = formId,
            label = "q$questionId",
            type = "text",
            order = order,
        ),
    )
}
