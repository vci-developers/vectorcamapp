package com.vci.vectorcamapp.landing.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class LandingViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val deviceCache: DeviceCache                = mockk(relaxed = true)
    private val currentSessionCache: CurrentSessionCache = mockk(relaxed = true)
    private val programRepo: ProgramRepository           = mockk(relaxed = true)
    private val sessionRepo: SessionRepository           = mockk(relaxed = true)
    private lateinit var vm: LandingViewModel

    private fun makeSession(type: SessionType) = Session(
        localId          = UUID.randomUUID(),
        remoteId         = null,
        houseNumber      = "1",
        collectorTitle   = "Dr.",
        collectorName    = "Alice",
        collectionDate   = 1_632_000_000L,
        collectionMethod = "Net",
        specimenCondition= "Good",
        createdAt        = 1_632_000_100L,
        completedAt      = null,
        submittedAt      = null,
        notes            = "",
        latitude         = null,
        longitude        = null,
        type             = type
    )

    @Test
    fun `when no programId, navigate back immediately`() = runTest {
        coEvery { deviceCache.getProgramId() } returns null
        every  { sessionRepo.observeIncompleteSessions() } returns flowOf(emptyList())

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val stateJob = launch { vm.state.collect { } }
        advanceUntilIdle()

        vm.events.test {
            assertThat(awaitItem())
                .isEqualTo(LandingEvent.NavigateBackToRegistrationScreen)
            cancelAndIgnoreRemainingEvents()
        }

        advanceUntilIdle()
        assertThat(vm.state.value.isLoading).isFalse()

        stateJob.cancel()
    }

    @Test
    fun `when programId present but programRepo returns null, navigate back`() = runTest {
        coEvery { deviceCache.getProgramId() } returns 1
        coEvery { programRepo.getProgramById(1) } returns null
        every  { sessionRepo.observeIncompleteSessions() } returns flowOf(emptyList())

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val stateJob = launch { vm.state.collect { } }
        advanceUntilIdle()

        vm.events.test {
            assertThat(awaitItem())
                .isEqualTo(LandingEvent.NavigateBackToRegistrationScreen)
            cancelAndIgnoreRemainingEvents()
        }

        advanceUntilIdle()
        assertThat(vm.state.value.isLoading).isFalse()

        stateJob.cancel()
    }

    @Test
    fun `when session exists, showResumeDialog is true`() = runTest {
        val dummy = makeSession(SessionType.DATA_COLLECTION)
        coEvery { deviceCache.getProgramId() } returns 1
        coEvery { programRepo.getProgramById(1) } returns Program(1, "Test", "XY")
        coEvery { currentSessionCache.getSession() } returns dummy
        every  { sessionRepo.observeIncompleteSessions() } returns flowOf(emptyList())

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val stateJob = launch { vm.state.collect { } }
        advanceUntilIdle()

        assertThat(vm.state.value.showResumeDialog).isTrue()
        assertThat(vm.state.value.enrolledProgram.name).isEqualTo("Test")

        stateJob.cancel()
    }

    @Test
    fun `resume action emits NavigateToIntakeScreen with correct type`() = runTest {
        val surv = makeSession(SessionType.SURVEILLANCE)
        coEvery { deviceCache.getProgramId() } returns 1
        coEvery { programRepo.getProgramById(1) } returns Program(1, "X", "ZZ")
        coEvery { currentSessionCache.getSession() } returns surv
        every  { sessionRepo.observeIncompleteSessions() } returns flowOf(emptyList())

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val stateJob = launch { vm.state.collect { } }
        advanceUntilIdle()

        vm.events.test {
            vm.onAction(LandingAction.ResumeSession)
            advanceUntilIdle()
            assertThat(awaitItem())
                .isEqualTo(LandingEvent.NavigateToIntakeScreen(SessionType.SURVEILLANCE))
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(vm.state.value.showResumeDialog).isFalse()

        stateJob.cancel()
    }

    @Test
    fun `dismiss resume clears session and hides dialog`() = runTest {
        val dummy = makeSession(SessionType.DATA_COLLECTION)
        coEvery { deviceCache.getProgramId() } returns 1
        coEvery { programRepo.getProgramById(1) } returns Program(1, "P", "CC")
        coEvery { currentSessionCache.getSession() } returns dummy
        every  { sessionRepo.observeIncompleteSessions() } returns flowOf(emptyList())

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val stateJob = launch { vm.state.collect { } }
        advanceUntilIdle()

        vm.onAction(LandingAction.DismissResumePrompt)
        advanceUntilIdle()

        coVerify { currentSessionCache.clearSession() }
        assertThat(vm.state.value.showResumeDialog).isFalse()

        stateJob.cancel()
    }

    @Test
    fun `action tiles emit correct events`() = runTest {
        coEvery { deviceCache.getProgramId() } returns 1
        coEvery { programRepo.getProgramById(1) } returns Program(1, "P", "CC")
        coEvery { currentSessionCache.getSession() } returns null
        val list = listOf(
            makeSession(SessionType.SURVEILLANCE),
            makeSession(SessionType.DATA_COLLECTION)
        )
        every { sessionRepo.observeIncompleteSessions() } returns flowOf(list)

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)
        advanceUntilIdle()

        vm.events.test {
            vm.onAction(LandingAction.StartNewSurveillanceSession)
            assertThat(awaitItem())
                .isEqualTo(LandingEvent.NavigateToIntakeScreen(SessionType.SURVEILLANCE))

            vm.onAction(LandingAction.StartNewDataCollectionSession)
            assertThat(awaitItem())
                .isEqualTo(LandingEvent.NavigateToIntakeScreen(SessionType.DATA_COLLECTION))

            vm.onAction(LandingAction.ViewIncompleteSessions)
            assertThat(awaitItem())
                .isEqualTo(LandingEvent.NavigateToIncompleteSessionsScreen)

            vm.onAction(LandingAction.ViewCompleteSessions)
            assertThat(awaitItem())
                .isEqualTo(LandingEvent.NavigateToCompleteSessionsScreen)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial load with valid program and no session keeps showResumeDialog false`() = runTest {
        coEvery { deviceCache.getProgramId() } returns 7
        coEvery { programRepo.getProgramById(7) } returns Program(7, "Valid", "US")
        coEvery { currentSessionCache.getSession() } returns null
        every  { sessionRepo.observeIncompleteSessions() } returns flowOf(emptyList())

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val stateJob = launch { vm.state.collect { } }
        advanceUntilIdle()

        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.showResumeDialog).isFalse()
        assertThat(vm.state.value.enrolledProgram.name).isEqualTo("Valid")

        stateJob.cancel()
    }

    @Test
    fun `incompleteSessionsCount updates when underlying flow emits`() = runTest {
        val incompleteFlow = MutableSharedFlow<List<Session>>(replay = 1).apply { tryEmit(emptyList()) }
        coEvery { deviceCache.getProgramId() } returns 5
        coEvery { programRepo.getProgramById(5) } returns Program(5, "Prog", "XX")
        coEvery { currentSessionCache.getSession() } returns null
        every { sessionRepo.observeIncompleteSessions() } returns incompleteFlow

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val counts = mutableListOf<Int>()
        val stateJob = launch { vm.state.collect { counts += it.incompleteSessionsCount } }
        advanceUntilIdle()

        incompleteFlow.emit(listOf(makeSession(SessionType.SURVEILLANCE)))
        incompleteFlow.emit(List(4) { makeSession(SessionType.DATA_COLLECTION) })
        advanceUntilIdle()

        assertThat(counts.last()).isEqualTo(4)

        stateJob.cancel()
    }
}
