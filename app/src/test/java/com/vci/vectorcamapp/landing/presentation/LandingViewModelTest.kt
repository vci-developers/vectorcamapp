package com.vci.vectorcamapp.landing.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.landing.domain.util.LandingError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
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
class LandingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var deviceCache: DeviceCache
    private lateinit var sessionCache: CurrentSessionCache
    private lateinit var programRepository: ProgramRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var errorMessageEmitter: ErrorMessageEmitter
    private lateinit var viewModel: LandingViewModel

    // Observed by VM to derive incompleteSessionsCount
    private lateinit var incompleteFlow: MutableStateFlow<List<SessionAndSite>>

    @Before
    fun setUp() {
        // Error bus
        errorMessageEmitter = mockk(relaxed = true)
        coEvery { errorMessageEmitter.emit(any(), any()) } returns Unit

        // Mocks
        deviceCache = mockk(relaxed = true)
        sessionCache = mockk(relaxed = true)
        programRepository = mockk()
        sessionRepository = mockk()

        // Default flow
        incompleteFlow = MutableStateFlow(emptyList())
        every { sessionRepository.observeIncompleteSessionsAndSites() } returns incompleteFlow
    }

    // ========================================
    // Test Harness / Helpers
    // ========================================

    private fun makeSession(type: SessionType) = Session(
        localId = UUID.randomUUID(),
        remoteId = null,
        hardwareId = "TEST123",
        collectorTitle = "Dr.",
        collectorName = "Alice",
        collectorLastTrainedOn = 0L,
        collectionDate = 1_632_000_000L,
        collectionMethod = "Net",
        specimenCondition = "Good",
        createdAt = 1_632_000_100L,
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        type = type,
    )

    private fun makeDummySite() = Site(
        id = 1,
        district = "Test District",
        subCounty = "Test SubCounty",
        parish = "Test Parish",
        villageName = "Test Village",
        houseNumber = "123",
        healthCenter = "Test Center",
        isActive = true
    )

    private fun initViewModel(
        programId: Int? = 1,
        program: Program? = Program(1, "Program A", "AA", "1.0.0"),
        currentSession: Session? = null
    ) {
        coEvery { deviceCache.getProgramId() } returns programId
        if (programId != null) {
            coEvery { programRepository.getProgramById(programId) } returns program
        }
        coEvery { sessionCache.getSession() } returns currentSession
        viewModel = LandingViewModel(
            deviceCache = deviceCache,
            currentSessionCache = sessionCache,
            programRepository = programRepository,
            sessionRepository = sessionRepository,
            errorMessageEmitter = errorMessageEmitter
        )
    }

    // ========================================
    // A. Initialization & State
    // ========================================

    @Test
    fun landVm_a01_noProgramId_navigatesBackAndStopsLoading() = runTest {
        initViewModel(programId = null)

        viewModel.state.test {
            awaitItem() // triggers onStart -> loadLandingDetails

            // Now that loading started, collect events
            viewModel.events.test {
                assertThat(awaitItem()).isEqualTo(LandingEvent.NavigateBackToRegistrationScreen)
                expectNoEvents()
            }

            val afterLoad = awaitItem()
            assertThat(afterLoad.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun landVm_a02_programMissing_navigatesBackAndStopsLoading() = runTest {
        initViewModel(programId = 7, program = null)

        viewModel.state.test {
            awaitItem()

            viewModel.events.test {
                assertThat(awaitItem()).isEqualTo(LandingEvent.NavigateBackToRegistrationScreen)
                expectNoEvents()
            }

            val after = awaitItem()
            assertThat(after.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun landVm_a03_validProgram_setsProgram_andNoResumeDialogWhenNoSession() = runTest {
        val program = Program(5, "Valid", "US", "1.0.0")
        initViewModel(programId = 5, program = program, currentSession = null)
        advanceUntilIdle()

        viewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.enrolledProgram).isEqualTo(program)
            assertThat(loaded.showResumeDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // B. Resume / Dismiss Resume
    // ========================================

    @Test
    fun landVm_b01_existingSession_showsResumeDialog() = runTest {
        val session = makeSession(SessionType.DATA_COLLECTION)
        initViewModel(programId = 1, program = Program(1, "P", "C", "1.0.0"), currentSession = session)
        advanceUntilIdle()

        viewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.showResumeDialog).isTrue()
            assertThat(loaded.enrolledProgram.name).isEqualTo("P")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun landVm_b02_resume_emitsNavigateWithCorrectType_andHidesDialog() = runTest {
        val session = makeSession(SessionType.SURVEILLANCE)
        initViewModel(programId = 1, program = Program(1, "P", "C", "1.0.0"), currentSession = session)

        viewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.showResumeDialog).isTrue()

            viewModel.events.test {
                viewModel.onAction(LandingAction.ResumeSession)
                assertThat(awaitItem()).isEqualTo(
                    LandingEvent.NavigateToIntakeScreen(SessionType.SURVEILLANCE)
                )
                expectNoEvents()
            }

            val after = awaitItem()
            assertThat(after.showResumeDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun landVm_b03_resume_whenNoSession_emitsError_andNoNavigation() = runTest {
        initViewModel(programId = 1, program = Program(1, "P", "C", "1.0.0"), currentSession = null)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(LandingAction.ResumeSession)
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 1) { errorMessageEmitter.emit(LandingError.SESSION_NOT_FOUND, any()) }
    }

    @Test
    fun landVm_b04_dismissResume_clearsSession_andHidesDialog() = runTest {
        val session = makeSession(SessionType.DATA_COLLECTION)
        initViewModel(programId = 1, program = Program(1, "P", "C", "1.0.0"), currentSession = session)

        viewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.showResumeDialog).isTrue()

            viewModel.onAction(LandingAction.DismissResumePrompt)

            val after = awaitItem()
            assertThat(after.showResumeDialog).isFalse()

            coVerify(exactly = 1) { sessionCache.clearSession() }

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // C. Action Tiles -> Events
    // ========================================

    @Test
    fun landVm_c01_actionTiles_emitCorrectEvents() = runTest {
        val dummySite = makeDummySite()
        initViewModel(programId = 1, program = Program(1, "P", "C", "1.0.0"), currentSession = null)
        incompleteFlow.value = listOf(
            SessionAndSite(makeSession(SessionType.SURVEILLANCE), dummySite),
            SessionAndSite(makeSession(SessionType.DATA_COLLECTION), dummySite)
        )
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(LandingAction.StartNewSurveillanceSession)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(
                LandingEvent.NavigateToIntakeScreen(SessionType.SURVEILLANCE)
            )

            viewModel.onAction(LandingAction.ViewIncompleteSessions)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(LandingEvent.NavigateToIncompleteSessionsScreen)

            viewModel.onAction(LandingAction.ViewCompleteSessions)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(LandingEvent.NavigateToCompleteSessionsScreen)

            expectNoEvents()
        }
    }

    // ========================================
    // D. Incomplete Count Updates
    // ========================================

    @Test
    fun landVm_d01_incompleteSessionsCount_updatesWithFlow() = runTest {
        val dummySite = makeDummySite()
        initViewModel(programId = 5, program = Program(5, "Prog", "XX", "1.0.0"), currentSession = null)

        viewModel.state.test {
            awaitItem()
            val s1 = awaitItem()
            assertThat(s1.incompleteSessionsCount).isEqualTo(0)

            incompleteFlow.value = listOf(SessionAndSite(makeSession(SessionType.SURVEILLANCE), dummySite))
            advanceUntilIdle()
            val s2 = awaitItem()
            assertThat(s2.incompleteSessionsCount).isEqualTo(1)

            incompleteFlow.value = List(4) { SessionAndSite(makeSession(SessionType.DATA_COLLECTION), dummySite)}
            advanceUntilIdle()
            val s3 = awaitItem()
            assertThat(s3.incompleteSessionsCount).isEqualTo(4)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // E. Ordering / Side-effects
    // ========================================

    @Test
    fun landVm_e01_dismissThenStartNew_ordersAreRespected_noExtraErrors() = runTest {
        val session = makeSession(SessionType.SURVEILLANCE)
        initViewModel(programId = 2, program = Program(2, "Q", "YY", "1.0.0"), currentSession = session)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(LandingAction.DismissResumePrompt)
            advanceUntilIdle()
            
            expectNoEvents()
        }

        coVerifyOrder {
            sessionCache.clearSession()
        }
        coVerify(exactly = 0) { errorMessageEmitter.emit(LandingError.SESSION_NOT_FOUND, any()) }
    }
}
