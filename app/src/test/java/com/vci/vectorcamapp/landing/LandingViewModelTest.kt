package com.vci.vectorcamapp.landing

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.landing.presentation.LandingAction
import com.vci.vectorcamapp.landing.presentation.LandingEvent
import com.vci.vectorcamapp.landing.presentation.LandingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mockito.*
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class LandingViewModelTest {

    @get:Rule
    val dispatcherRule = StandardTestDispatcherRule()

    private lateinit var deviceCache: DeviceCache
    private lateinit var currentSessionCache: CurrentSessionCache
    private lateinit var programRepo: ProgramRepository
    private lateinit var sessionRepo: SessionRepository
    private lateinit var vm: LandingViewModel

    private fun makeSession(type: SessionType) = Session(
        localId      = UUID.randomUUID(),
        remoteId     = null,
        houseNumber  = "1",
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

    @Before fun setup() {
        deviceCache         = mock(DeviceCache::class.java)
        currentSessionCache = mock(CurrentSessionCache::class.java)
        programRepo         = mock(ProgramRepository::class.java)
        sessionRepo         = mock(SessionRepository::class.java)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when no programId, navigate back immediately`() = runTest {
        `when`(deviceCache.getProgramId()).thenReturn(null)
        `when`(sessionRepo.observeIncompleteSessions())
            .thenReturn(flowOf(emptyList<Session>()))

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
        `when`(deviceCache.getProgramId()).thenReturn(1)
        `when`(programRepo.getProgramById(1)).thenReturn(null)
        `when`(sessionRepo.observeIncompleteSessions())
            .thenReturn(flowOf(emptyList()))

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val stateJob = launch { vm.state.collect {} }
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
        `when`(deviceCache.getProgramId()).thenReturn(1)
        `when`(programRepo.getProgramById(1))
            .thenReturn(Program(id = 1, name = "Test", country = "XY"))

        val dummy = makeSession(SessionType.DATA_COLLECTION)
        `when`(currentSessionCache.getSession()).thenReturn(dummy)
        `when`(sessionRepo.observeIncompleteSessions())
            .thenReturn(flowOf(emptyList()))

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val stateJob = launch { vm.state.collect { } }

        advanceUntilIdle()

        assertThat(vm.state.value.showResumeDialog).isTrue()
        assertThat(vm.state.value.enrolledProgram.name).isEqualTo("Test")

        stateJob.cancel()
    }


    @Test fun `resume action emits NavigateToIntakeScreen with correct type`() = runTest {
        `when`(deviceCache.getProgramId()).thenReturn(1)
        `when`(programRepo.getProgramById(1))
            .thenReturn(Program(id = 1, name = "X", country = "ZZ"))

        val dummy = makeSession(SessionType.SURVEILLANCE)
        `when`(currentSessionCache.getSession()).thenReturn(dummy)
        `when`(sessionRepo.observeIncompleteSessions())
            .thenReturn(flowOf(emptyList()))

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)
        advanceUntilIdle()

        vm.events.test {
            vm.onAction(LandingAction.ResumeSession)
            assertThat(awaitItem())
                .isEqualTo(LandingEvent.NavigateToIntakeScreen(SessionType.SURVEILLANCE))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.state.value.showResumeDialog).isFalse()
    }

    @Test
    fun `dismiss resume clears session and hides dialog`() = runTest {
        `when`(deviceCache.getProgramId()).thenReturn(1)
        `when`(programRepo.getProgramById(1))
            .thenReturn(Program(id = 1, name = "P", country = "CC"))

        val dummy = makeSession(SessionType.DATA_COLLECTION)
        `when`(currentSessionCache.getSession()).thenReturn(dummy)
        `when`(sessionRepo.observeIncompleteSessions())
            .thenReturn(flowOf(emptyList<Session>()))

        vm = LandingViewModel(deviceCache, currentSessionCache, programRepo, sessionRepo)

        val stateJob = launch { vm.state.collect { } }
        advanceUntilIdle()

        vm.onAction(LandingAction.DismissResumePrompt)
        advanceUntilIdle()

        // assert
        verify(currentSessionCache).clearSession()
        assertThat(vm.state.value.showResumeDialog).isFalse()

        stateJob.cancel()
    }


    @Test fun `action tiles emit correct events`() = runTest {
        `when`(deviceCache.getProgramId()).thenReturn(1)
        `when`(programRepo.getProgramById(1))
            .thenReturn(Program(id = 1, name = "P", country = "CC"))

        `when`(currentSessionCache.getSession()).thenReturn(null)
        val list = listOf(
            makeSession(SessionType.SURVEILLANCE),
            makeSession(SessionType.DATA_COLLECTION)
        )
        `when`(sessionRepo.observeIncompleteSessions())
            .thenReturn(flowOf(list))

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
}

@OptIn(ExperimentalCoroutinesApi::class)
class StandardTestDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        Dispatchers.resetMain()
    }
}
