package com.vci.vectorcamapp.collection_batch.list.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
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
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.logging.analytics.VectorCamAnalytics
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.navigation.Destination
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionBatchListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionId = UUID.fromString("00000000-0000-0000-0000-0000000000aa")
    private val unitId = UUID.fromString("00000000-0000-0000-0000-0000000000bb")

    private lateinit var deviceCache: DeviceCache
    private lateinit var currentSessionCache: CurrentSessionCache
    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionUnitRepository: SessionUnitRepository
    private lateinit var specimenImageRepository: SpecimenImageRepository
    private lateinit var programRepository: ProgramRepository
    private lateinit var formRepository: FormRepository
    private lateinit var formQuestionRepository: FormQuestionRepository
    private lateinit var formAnswerRepository: FormAnswerRepository
    private lateinit var workManagerRepository: WorkManagerRepository
    private lateinit var viewModel: CollectionBatchListViewModel

    private val sessionUnits = MutableStateFlow<List<SessionUnit>>(emptyList())
    private val answersByUnit = MutableStateFlow<Map<UUID, Map<Int, FormAnswer>>>(emptyMap())

    @Before
    fun setUp() {
        VectorCamAnalytics.analytics = null
        VectorCamAnalytics.enabled = true
        deviceCache = mockk(relaxed = true)
        currentSessionCache = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        sessionUnitRepository = mockk(relaxed = true)
        specimenImageRepository = mockk(relaxed = true)
        programRepository = mockk(relaxed = true)
        formRepository = mockk(relaxed = true)
        formQuestionRepository = mockk(relaxed = true)
        formAnswerRepository = mockk(relaxed = true)
        workManagerRepository = mockk(relaxed = true)
        every { sessionUnitRepository.observeSessionUnitsForSession(sessionId) } returns sessionUnits
        every { formAnswerRepository.observeSessionUnitScopedFormAnswersBySessionId(sessionId) } returns answersByUnit
        coEvery { specimenImageRepository.getTotalCountForSession(sessionId) } returns 6
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        VectorCamAnalytics.analytics = null
    }

    @Test
    fun load_mapsUnitsCountsAndBucketNames() = runTest {
        val unit = SessionUnit(unitId, remoteId = null, unitOrder = 1, createdAt = 10L)
        stubIdentityQuestions()
        coEvery { sessionUnitRepository.countSpecimensForSessionUnit(unitId) } returns 3
        answersByUnit.value = mapOf(
            unitId to mapOf(1 to answer("Kampala"), 2 to answer("12")),
        )
        sessionUnits.value = listOf(unit)
        initViewModel()

        viewModel.state.test {
            val loaded = awaitItemWithUnits()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.sessionId).isEqualTo(sessionId)
            assertThat(loaded.sessionUnits).containsExactly(unit)
            assertThat(loaded.specimenCountsBySessionUnitId).containsEntry(unitId, 3)
            assertThat(loaded.bucketNamesBySessionUnitId).containsEntry(unitId, "Kampala · 12")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unknownProgram_leavesBucketNameBlank() = runTest {
        val unit = SessionUnit(unitId, remoteId = null, unitOrder = 1, createdAt = 10L)
        coEvery { deviceCache.getProgramId() } returns 7
        coEvery { programRepository.getProgramById(7) } returns null
        sessionUnits.value = listOf(unit)
        initViewModel()

        viewModel.state.test {
            val loaded = awaitItemWithUnits()
            assertThat(loaded.bucketNamesBySessionUnitId).containsEntry(unitId, "")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun programWithoutFormVersion_leavesBucketNameBlank() = runTest {
        val unit = SessionUnit(unitId, remoteId = null, unitOrder = 1, createdAt = 10L)
        coEvery { deviceCache.getProgramId() } returns 7
        coEvery { programRepository.getProgramById(7) } returns Program(7, "Program", "UG", null)
        sessionUnits.value = listOf(unit)
        initViewModel()

        viewModel.state.test {
            val loaded = awaitItemWithUnits()
            assertThat(loaded.bucketNamesBySessionUnitId).containsEntry(unitId, "")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun programWithoutForm_leavesBucketNameBlank() = runTest {
        val unit = SessionUnit(unitId, remoteId = null, unitOrder = 1, createdAt = 10L)
        coEvery { deviceCache.getProgramId() } returns 7
        coEvery { programRepository.getProgramById(7) } returns Program(7, "Program", "UG", "v1")
        coEvery { formRepository.getFormByVersion("v1") } returns null
        sessionUnits.value = listOf(unit)
        initViewModel()

        viewModel.state.test {
            val loaded = awaitItemWithUnits()
            assertThat(loaded.bucketNamesBySessionUnitId).containsEntry(unitId, "")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingProgram_leavesBucketNameBlank() = runTest {
        val unit = SessionUnit(unitId, remoteId = null, unitOrder = 1, createdAt = 10L)
        coEvery { deviceCache.getProgramId() } returns null
        sessionUnits.value = listOf(unit)
        initViewModel()

        viewModel.state.test {
            val loaded = awaitItemWithUnits()
            assertThat(loaded.bucketNamesBySessionUnitId).containsEntry(unitId, "")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addAndEdit_navigateToTheForm() = runTest {
        initViewModel()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchListAction.AddCollectionBatch)
            assertThat(awaitItem()).isEqualTo(
                CollectionBatchListEvent.NavigateToCollectionBatchForm(sessionId, null)
            )

            viewModel.onAction(CollectionBatchListAction.EditCollectionBatch(unitId))
            assertThat(awaitItem()).isEqualTo(
                CollectionBatchListEvent.NavigateToCollectionBatchForm(sessionId, unitId)
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun submitDialog_opensSelectsAndDismisses() = runTest {
        initViewModel()

        viewModel.state.test {
            awaitItem()
            awaitItem()

            viewModel.onAction(CollectionBatchListAction.OpenSubmitDialog)
            assertThat(awaitItem().isSubmitDialogVisible).isTrue()

            viewModel.onAction(
                CollectionBatchListAction.SelectPendingAction(CollectionBatchListAction.SaveSessionProgress)
            )
            assertThat(awaitItem().submissionPendingAction)
                .isEqualTo(CollectionBatchListAction.SaveSessionProgress)

            viewModel.onAction(CollectionBatchListAction.ClearPendingAction)
            assertThat(awaitItem().submissionPendingAction).isNull()

            viewModel.onAction(CollectionBatchListAction.DismissSubmitDialog)
            val dismissed = awaitItem()
            assertThat(dismissed.isSubmitDialogVisible).isFalse()
            assertThat(dismissed.submissionPendingAction).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmPendingAction_withoutSelection_onlyClosesTheDialog() = runTest {
        initViewModel()

        viewModel.state.test {
            awaitItem()
            awaitItem()
            viewModel.onAction(CollectionBatchListAction.OpenSubmitDialog)
            awaitItem()

            viewModel.onAction(CollectionBatchListAction.ConfirmPendingAction)
            assertThat(awaitItem().isSubmitDialogVisible).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmPendingAction_runsTheSelectedNavigation() = runTest {
        initViewModel()
        viewModel.state.test {
            awaitItem()
            awaitItem()
            viewModel.onAction(
                CollectionBatchListAction.SelectPendingAction(CollectionBatchListAction.AddCollectionBatch)
            )
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.events.test {
            viewModel.onAction(CollectionBatchListAction.ConfirmPendingAction)
            assertThat(awaitItem()).isEqualTo(
                CollectionBatchListEvent.NavigateToCollectionBatchForm(sessionId, null)
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveProgress_clearsSessionAndReturnsToLanding() = runTest {
        initViewModel()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchListAction.SaveSessionProgress)
            assertThat(awaitItem()).isEqualTo(CollectionBatchListEvent.NavigateBackToLandingScreen)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { currentSessionCache.clearSession() }
        coVerify { specimenImageRepository.getTotalCountForSession(sessionId) }
    }

    @Test
    fun confirmSubmit_withoutSession_returnsToLanding() = runTest {
        coEvery { currentSessionCache.getSession() } returns null
        coEvery { currentSessionCache.getSiteId() } returns 4
        initViewModel()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchListAction.ConfirmSubmitSession)
            assertThat(awaitItem()).isEqualTo(CollectionBatchListEvent.NavigateBackToLandingScreen)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { sessionRepository.markSessionAsComplete(any()) }
    }

    @Test
    fun confirmSubmit_success_enqueuesUploadAndReturnsToLanding() = runTest {
        coEvery { currentSessionCache.getSession() } returns session()
        coEvery { currentSessionCache.getSiteId() } returns 4
        coEvery { sessionRepository.markSessionAsComplete(sessionId) } returns true
        initViewModel()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchListAction.ConfirmSubmitSession)
            assertThat(awaitItem()).isEqualTo(CollectionBatchListEvent.NavigateBackToLandingScreen)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { workManagerRepository.enqueueSessionUpload(sessionId, 4) }
        coVerify { currentSessionCache.clearSession() }
    }

    @Test
    fun confirmSubmit_whenCompleteFails_staysPut() = runTest {
        coEvery { currentSessionCache.getSession() } returns session()
        coEvery { currentSessionCache.getSiteId() } returns 4
        coEvery { sessionRepository.markSessionAsComplete(sessionId) } returns false
        initViewModel()

        viewModel.events.test {
            viewModel.onAction(CollectionBatchListAction.ConfirmSubmitSession)
            advanceUntilIdle()
            expectNoEvents()
        }
        coVerify(exactly = 0) { workManagerRepository.enqueueSessionUpload(any(), any()) }
        coVerify(exactly = 0) { currentSessionCache.clearSession() }
    }

    private suspend fun ReceiveTurbine<CollectionBatchListState>.awaitItemWithUnits(): CollectionBatchListState {
        var latest = awaitItem()
        while (latest.sessionUnits.isEmpty()) {
            latest = awaitItem()
        }
        return latest
    }

    private fun initViewModel() {
        val savedStateHandle = mockk<SavedStateHandle>(relaxed = true)
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { savedStateHandle.toRoute<Destination.CollectionBatchList>() } returns
            Destination.CollectionBatchList(sessionId.toString())

        viewModel = CollectionBatchListViewModel(
            savedStateHandle = savedStateHandle,
            deviceCache = deviceCache,
            currentSessionCache = currentSessionCache,
            sessionRepository = sessionRepository,
            sessionUnitRepository = sessionUnitRepository,
            specimenImageRepository = specimenImageRepository,
            programRepository = programRepository,
            formRepository = formRepository,
            formQuestionRepository = formQuestionRepository,
            formAnswerRepository = formAnswerRepository,
            workManagerRepository = workManagerRepository,
            errorMessageEmitter = mockk(relaxed = true),
        )
    }

    private fun stubIdentityQuestions() {
        coEvery { deviceCache.getProgramId() } returns 7
        coEvery { programRepository.getProgramById(7) } returns Program(7, "Program", "UG", "v1")
        coEvery { formRepository.getFormByVersion("v1") } returns Form(3, "Form", "v1")
        coEvery {
            formQuestionRepository.getQuestionsByFormIdAndScope(3, FormQuestionScope.SESSION_UNIT)
        } returns listOf(
            FormQuestion(1, "Village", "text", false, null, null, 1, FormQuestionScope.SESSION_UNIT, true),
            FormQuestion(2, "House", "text", false, null, null, 2, FormQuestionScope.SESSION_UNIT, true),
        )
    }

    private fun answer(value: String) = FormAnswer(
        localId = UUID.randomUUID(),
        remoteId = null,
        value = value,
        dataType = "text",
        submittedAt = 0L,
    )

    private fun session() = Session(
        localId = sessionId,
        remoteId = null,
        hardwareId = "HW",
        collectorTitle = "VCO",
        collectorName = "Ada",
        collectorLastTrainedOn = 0L,
        collectionDate = 1L,
        collectionMethod = "Net",
        specimenCondition = "Good",
        createdAt = 2L,
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        type = SessionType.SURVEILLANCE,
    )
}
