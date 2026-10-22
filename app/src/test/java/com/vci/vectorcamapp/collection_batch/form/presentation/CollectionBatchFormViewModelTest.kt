package com.vci.vectorcamapp.collection_batch.form.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.collection_batch.domain.use_cases.CollectionBatchFormValidationUseCases
import com.vci.vectorcamapp.collection_batch.domain.use_cases.ValidateCollectionBatchIdentityUseCase
import com.vci.vectorcamapp.collection_batch.domain.use_cases.ValidateFormAnswersUseCase
import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Form
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import com.vci.vectorcamapp.intake.domain.strategy.program_form.ProgramFormWorkflowFactory
import com.vci.vectorcamapp.navigation.Destination
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionBatchFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val editingUnitId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val otherUnitId = UUID.fromString("00000000-0000-0000-0000-000000000003")

    private lateinit var deviceCache: DeviceCache
    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionUnitRepository: SessionUnitRepository
    private lateinit var programRepository: ProgramRepository
    private lateinit var formRepository: FormRepository
    private lateinit var formQuestionRepository: FormQuestionRepository
    private lateinit var formAnswerRepository: FormAnswerRepository
    private lateinit var errorMessageEmitter: ErrorMessageEmitter
    private lateinit var transactionHelper: TransactionHelper
    private lateinit var viewModel: CollectionBatchFormViewModel

    private val existingAnswers = MutableStateFlow<Map<UUID, Map<Int, FormAnswer>>>(emptyMap())

    @Before
    fun setUp() {
        deviceCache = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        coEvery { sessionRepository.getSessionById(sessionId) } returns session(SessionType.SURVEILLANCE)
        sessionUnitRepository = mockk(relaxed = true)
        programRepository = mockk(relaxed = true)
        formRepository = mockk(relaxed = true)
        formQuestionRepository = mockk(relaxed = true)
        formAnswerRepository = mockk(relaxed = true)
        errorMessageEmitter = mockk(relaxed = true)
        transactionHelper = mockk()
        coEvery { transactionHelper.runAsTransaction<Boolean>(any()) } coAnswers {
            firstArg<suspend () -> Boolean>().invoke()
        }
        every { formAnswerRepository.observeSessionUnitScopedFormAnswersBySessionId(sessionId) } returns existingAnswers
        coEvery { errorMessageEmitter.emit(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
    }

    @Test
    fun missingProgramId_finishesLoadingWithNoQuestions() = runTest {
        coEvery { deviceCache.getProgramId() } returns null
        initViewModel()

        viewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.formQuestions).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingProgram_finishesLoadingWithNoQuestions() = runTest {
        coEvery { deviceCache.getProgramId() } returns 7
        coEvery { programRepository.getProgramById(7) } returns null
        initViewModel()

        viewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.formQuestions).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun programWithoutForm_finishesLoadingWithNoQuestions() = runTest {
        coEvery { deviceCache.getProgramId() } returns 7
        coEvery { programRepository.getProgramById(7) } returns program(formVersion = null)
        initViewModel()

        viewModel.state.test {
            awaitItem()
            assertThat(awaitItem().formQuestions).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { formRepository.getFormByVersion(any()) }
    }

    @Test
    fun load_usesSavedAnswersAndBooleanDefaults() = runTest {
        val saved = answer("kept", "text")
        stubForm(listOf(question(1, type = "text"), question(2, type = "boolean")))
        coEvery { formAnswerRepository.getSessionUnitScopedFormAnswers(editingUnitId) } returns mapOf(1 to saved)
        initViewModel(sessionUnitId = editingUnitId)

        viewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.formAnswersByQuestionId.getValue(1)).isEqualTo(saved)
            assertThat(loaded.formAnswersByQuestionId.getValue(2).value).isEqualTo("false")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun load_dataCollectionSession_keepsOnlyIdentityQuestions() = runTest {
        coEvery { sessionRepository.getSessionById(sessionId) } returns
            session(SessionType.DATA_COLLECTION)
        stubForm(
            listOf(
                question(1, identity = true),
                question(2, identity = false),
                question(3, identity = true),
            )
        )
        initViewModel()

        viewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.formQuestions.map { it.id }).containsExactly(1, 3).inOrder()
            assertThat(loaded.formAnswersByQuestionId.keys).containsExactly(1, 3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun load_surveillanceSession_keepsEveryQuestion() = runTest {
        coEvery { sessionRepository.getSessionById(sessionId) } returns
            session(SessionType.SURVEILLANCE)
        stubForm(listOf(question(1, identity = true), question(2, identity = false)))
        initViewModel()

        viewModel.state.test {
            awaitItem()
            assertThat(awaitItem().formQuestions.map { it.id }).containsExactly(1, 2).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun load_practiceSession_mirrorsSurveillanceAndKeepsEveryQuestion() = runTest {
        coEvery { sessionRepository.getSessionById(sessionId) } returns session(SessionType.PRACTICE)
        stubForm(listOf(question(1, identity = true), question(2, identity = false)))
        initViewModel()

        viewModel.state.test {
            awaitItem()
            assertThat(awaitItem().formQuestions.map { it.id }).containsExactly(1, 2).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun submit_dataCollectionSession_ignoresRequiredNonIdentityQuestion() = runTest {
        coEvery { sessionRepository.getSessionById(sessionId) } returns
            session(SessionType.DATA_COLLECTION)
        stubForm(
            listOf(
                question(1, identity = true),
                question(2, required = true, identity = false),
            )
        )
        coEvery { sessionUnitRepository.getSessionUnitById(any()) } returns null
        coEvery { sessionUnitRepository.getMaxSessionUnitOrderForSession(sessionId) } returns 0
        coEvery { sessionUnitRepository.upsertSessionUnit(any(), any()) } returns Result.Success(Unit)
        coEvery { formAnswerRepository.upsertFormAnswer(any(), any(), any(), any()) } returns
            Result.Success(Unit)
        initViewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(CollectionBatchFormAction.UpdateFormAnswer(1, "House"))
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchFormAction.SubmitSessionUnitForm)
            val event = awaitItem() as CollectionBatchFormEvent.NavigateToImagingScreen
            coVerify {
                formAnswerRepository.upsertFormAnswer(any(), sessionId, event.sessionUnitId, 1)
            }
            coVerify(exactly = 0) {
                formAnswerRepository.upsertFormAnswer(any(), any(), any(), 2)
            }
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) {
            errorMessageEmitter.emit(CollectionBatchFormError.FORM_INVALID, any())
        }
    }

    @Test
    fun updateFormAnswer_replacesValueAndClearsHiddenDependents() = runTest {
        val requiresYes = FormQuestionPrerequisiteExpression.Predicate(
            questionId = 1,
            operator = "eq",
            value = FormQuestionPrerequisiteValue.StringValue("yes"),
        )
        val booleanFollowUp = question(id = 2, type = "boolean", prerequisite = requiresYes)
        val textFollowUp = question(id = 3, type = "text", prerequisite = requiresYes)
        stubForm(listOf(question(1), booleanFollowUp, textFollowUp))
        initViewModel()

        viewModel.state.test {
            awaitItem()
            awaitItem()

            viewModel.onAction(CollectionBatchFormAction.UpdateFormAnswer(2, "true"))
            assertThat(awaitItem().formAnswersByQuestionId.getValue(2).value).isEqualTo("true")
            viewModel.onAction(CollectionBatchFormAction.UpdateFormAnswer(3, "note"))
            assertThat(awaitItem().formAnswersByQuestionId.getValue(3).value).isEqualTo("note")

            viewModel.onAction(CollectionBatchFormAction.UpdateFormAnswer(1, "no"))
            val cleared = awaitItem()
            assertThat(cleared.formAnswersByQuestionId.getValue(2).value).isEqualTo("false")
            assertThat(cleared.formAnswersByQuestionId.getValue(3).value).isEqualTo("")

            viewModel.onAction(CollectionBatchFormAction.UpdateFormAnswer(99, "ignored"))
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun return_navigatesBack() = runTest {
        initViewModel()
        backgroundScope.launch { viewModel.state.collect {} }

        viewModel.events.test {
            viewModel.onAction(CollectionBatchFormAction.ReturnToCollectionBatchListScreen)
            assertThat(awaitItem())
                .isEqualTo(CollectionBatchFormEvent.NavigateBackToCollectionBatchListScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun submit_invalidAnswer_emitsFormInvalid() = runTest {
        stubForm(listOf(question(1, required = true)))
        initViewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(CollectionBatchFormAction.SubmitSessionUnitForm)
        advanceUntilIdle()

        coVerify { errorMessageEmitter.emit(CollectionBatchFormError.FORM_INVALID, any()) }
        coVerify(exactly = 0) { transactionHelper.runAsTransaction<Boolean>(any()) }
    }

    @Test
    fun submit_duplicateIdentity_emitsDuplicate() = runTest {
        stubForm(listOf(question(1, identity = true)))
        initViewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(CollectionBatchFormAction.UpdateFormAnswer(1, "House"))
        existingAnswers.value = mapOf(otherUnitId to mapOf(1 to answer("House")))
        advanceUntilIdle()

        viewModel.onAction(CollectionBatchFormAction.SubmitSessionUnitForm)
        advanceUntilIdle()

        coVerify { errorMessageEmitter.emit(CollectionBatchFormError.DUPLICATE_IDENTITY, any()) }
        coVerify(exactly = 0) { transactionHelper.runAsTransaction<Boolean>(any()) }
    }

    @Test
    fun submit_newUnit_persistsAndNavigatesToImaging() = runTest {
        stubForm(listOf(question(1)))
        coEvery { sessionUnitRepository.getSessionUnitById(any()) } returns null
        coEvery { sessionUnitRepository.getMaxSessionUnitOrderForSession(sessionId) } returns 4
        coEvery { sessionUnitRepository.upsertSessionUnit(any(), any()) } returns Result.Success(Unit)
        coEvery { formAnswerRepository.upsertFormAnswer(any(), any(), any(), any()) } returns Result.Success(Unit)
        initViewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(CollectionBatchFormAction.UpdateFormAnswer(1, "House"))
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchFormAction.SubmitSessionUnitForm)
            val event = awaitItem() as CollectionBatchFormEvent.NavigateToImagingScreen
            coVerify {
                sessionUnitRepository.upsertSessionUnit(
                    match { it.localId == event.sessionUnitId && it.unitOrder == 5 },
                    sessionId,
                )
            }
            coVerify {
                formAnswerRepository.upsertFormAnswer(any(), sessionId, event.sessionUnitId, 1)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun submit_existingUnit_reusesSavedUnit() = runTest {
        val existing = SessionUnit(editingUnitId, remoteId = 8, unitOrder = 2, createdAt = 50L)
        stubForm(listOf(question(1)))
        coEvery { sessionUnitRepository.getSessionUnitById(editingUnitId) } returns existing
        coEvery { sessionUnitRepository.upsertSessionUnit(any(), any()) } returns Result.Success(Unit)
        coEvery { formAnswerRepository.upsertFormAnswer(any(), any(), any(), any()) } returns Result.Success(Unit)
        initViewModel(sessionUnitId = editingUnitId)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchFormAction.SubmitSessionUnitForm)
            val event = awaitItem() as CollectionBatchFormEvent.NavigateToImagingScreen
            assertThat(event.sessionUnitId).isEqualTo(editingUnitId)
            coVerify { sessionUnitRepository.upsertSessionUnit(existing, sessionId) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun submit_repositoryError_doesNotNavigate() = runTest {
        stubForm(listOf(question(1)))
        coEvery { sessionUnitRepository.upsertSessionUnit(any(), any()) } returns
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        initViewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchFormAction.SubmitSessionUnitForm)
            advanceUntilIdle()
            expectNoEvents()
        }
        coVerify { errorMessageEmitter.emit(RoomDbError.UNKNOWN_ERROR, any()) }
    }

    @Test
    fun submit_answerSaveError_doesNotNavigate() = runTest {
        stubForm(listOf(question(1)))
        coEvery { sessionUnitRepository.upsertSessionUnit(any(), any()) } returns Result.Success(Unit)
        coEvery { formAnswerRepository.upsertFormAnswer(any(), any(), any(), any()) } returns
            Result.Error(RoomDbError.CONSTRAINT_VIOLATION)
        initViewModel()
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchFormAction.SubmitSessionUnitForm)
            advanceUntilIdle()
            expectNoEvents()
        }
        coVerify { errorMessageEmitter.emit(RoomDbError.CONSTRAINT_VIOLATION, any()) }
    }

    private fun initViewModel(sessionUnitId: UUID? = null) {
        val savedStateHandle = mockk<SavedStateHandle>(relaxed = true)
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { savedStateHandle.toRoute<Destination.CollectionBatchForm>() } returns
            Destination.CollectionBatchForm(sessionId.toString(), sessionUnitId?.toString())

        viewModel = CollectionBatchFormViewModel(
            savedStateHandle = savedStateHandle,
            deviceCache = deviceCache,
            sessionRepository = sessionRepository,
            sessionUnitRepository = sessionUnitRepository,
            programRepository = programRepository,
            programFormWorkflowFactory = ProgramFormWorkflowFactory(
                formRepository = formRepository,
                formQuestionRepository = formQuestionRepository,
            ),
            formAnswerRepository = formAnswerRepository,
            collectionBatchFormValidationUseCases = CollectionBatchFormValidationUseCases(
                validateFormAnswers = ValidateFormAnswersUseCase(),
                validateCollectionBatchIdentity = ValidateCollectionBatchIdentityUseCase(),
            ),
            errorMessageEmitter = errorMessageEmitter,
        ).also { it.transactionHelper = transactionHelper }
    }

    private fun stubForm(questions: List<FormQuestion>) {
        coEvery { deviceCache.getProgramId() } returns 7
        coEvery { programRepository.getProgramById(7) } returns program(formVersion = "v1")
        coEvery { formRepository.getFormByVersion("v1") } returns Form(id = 3, name = "Form", version = "v1")
        coEvery {
            formQuestionRepository.getQuestionsByFormIdAndScope(3, FormQuestionScope.SESSION_UNIT)
        } returns questions
    }

    private fun session(type: SessionType) = Session(
        localId = sessionId,
        remoteId = null,
        hardwareId = null,
        collectorTitle = "Title",
        collectorName = "Name",
        collectorLastTrainedOn = 0L,
        collectionDate = 0L,
        collectionMethod = "Human Landing Catch (HLC)",
        specimenCondition = "Fresh",
        createdAt = 0L,
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        gpsAccuracy = null,
        type = type,
    )

    private fun program(formVersion: String?) = Program(
        id = 7,
        name = "Program",
        country = "UG",
        formVersion = formVersion,
    )

    private fun question(
        id: Int,
        type: String = "text",
        required: Boolean = false,
        identity: Boolean = false,
        prerequisite: FormQuestionPrerequisiteExpression? = null,
    ) = FormQuestion(
        id = id,
        label = "q$id",
        type = type,
        required = required,
        prerequisite = prerequisite,
        options = null,
        order = id,
        answerScope = FormQuestionScope.SESSION_UNIT,
        isUnitIdentityComponent = identity,
    )

    private fun answer(value: String, type: String = "text") = FormAnswer(
        localId = UUID.randomUUID(),
        remoteId = null,
        value = value,
        dataType = type,
        submittedAt = 0L,
    )
}
