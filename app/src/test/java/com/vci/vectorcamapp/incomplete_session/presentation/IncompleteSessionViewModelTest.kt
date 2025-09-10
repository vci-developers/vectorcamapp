package com.vci.vectorcamapp.incomplete_session.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageBus
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.incomplete_session.domain.util.IncompleteSessionError
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
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
class IncompleteSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionRepository: SessionRepository
    private lateinit var currentSessionCache: CurrentSessionCache
    private lateinit var viewModel: IncompleteSessionViewModel

    private lateinit var incompleteSessionsFlow: MutableStateFlow<List<Session>>

    @Before
    fun setUp() {
        mockkObject(ErrorMessageBus)
        coEvery { ErrorMessageBus.emit(any(), any()) } returns Unit

        sessionRepository = mockk()
        currentSessionCache = mockk(relaxed = true)

        incompleteSessionsFlow = MutableStateFlow(emptyList())
        every { sessionRepository.observeIncompleteSessions() } returns incompleteSessionsFlow

        viewModel = IncompleteSessionViewModel(
            sessionRepository = sessionRepository,
            currentSessionCache = currentSessionCache
        )
    }

    @After
    fun tearDown() {
        unmockkObject(ErrorMessageBus)
    }

    // ========================================
    // Test Harness / Helpers
    // ========================================

    private fun makeSession(sessionType: SessionType) = Session(
        localId = UUID.randomUUID(),
        remoteId = null,
        houseNumber = "1",
        collectorTitle = "Dr.",
        collectorName = "Alice",
        collectionDate = 1_632_000_000L,
        collectionMethod = "Net",
        specimenCondition = "Good",
        createdAt = 1_632_000_100L,
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        type = sessionType
    )

    private fun makeSessionAndSite(session: Session, siteId: Int): SessionAndSite {
        val sessionAndSite = mockk<SessionAndSite>(relaxed = true)
        every { sessionAndSite.session } returns session
        every { sessionAndSite.site.id } returns siteId
        return sessionAndSite
    }

    // ========================================
    // A. Initialization & State
    // ========================================

    @Test
    fun incVm_a01_initialStateHasEmptySessions() = runTest {
        advanceUntilIdle()

        viewModel.state.test {
            val initialState = awaitItem()
            assertThat(initialState.sessions).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun incVm_a02_stateUpdatesWhenRepositoryEmits() = runTest {
        advanceUntilIdle()

        viewModel.state.test {
            awaitItem()

            val newSessions = listOf(
                makeSession(SessionType.SURVEILLANCE),
                makeSession(SessionType.DATA_COLLECTION)
            )
            incompleteSessionsFlow.value = newSessions
            advanceUntilIdle()

            val updatedState = awaitItem()
            assertThat(updatedState.sessions).isEqualTo(newSessions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // B. Resume Session Action
    // ========================================

    @Test
    fun incVm_b01_resumeSession_success_savesToCache_andEmitsNavigateEvent() = runTest {
        val session = makeSession(SessionType.SURVEILLANCE)
        val siteId = 42
        val sessionAndSite = makeSessionAndSite(session, siteId)

        coEvery { sessionRepository.getSessionAndSiteById(session.localId) } returns sessionAndSite

        viewModel.events.test {
            viewModel.onAction(IncompleteSessionAction.ResumeSession(session.localId))
            val event = awaitItem()
            assertThat(event).isEqualTo(
                IncompleteSessionEvent.NavigateToIntakeScreen(SessionType.SURVEILLANCE)
            )
            expectNoEvents()
        }

        coVerify(exactly = 1) { currentSessionCache.saveSession(session, siteId) }
    }

    @Test
    fun incVm_b02_resumeSession_sessionNotFound_emitsError_andNoEvent_andDoesNotSave() = runTest {
        val randomId = UUID.randomUUID()
        coEvery { sessionRepository.getSessionAndSiteById(randomId) } returns null

        viewModel.events.test {
            viewModel.onAction(IncompleteSessionAction.ResumeSession(randomId))
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(IncompleteSessionError.SESSION_NOT_FOUND, any()) }
        coVerify(exactly = 0) { currentSessionCache.saveSession(any(), any()) }
    }

    @Test
    fun incVm_b03_resumeSession_repositoryThrows_emitsRetrievalFailed_andNoEvent_andDoesNotSave() = runTest {
        val randomId = UUID.randomUUID()
        coEvery { sessionRepository.getSessionAndSiteById(randomId) } throws RuntimeException("boom")

        viewModel.events.test {
            viewModel.onAction(IncompleteSessionAction.ResumeSession(randomId))
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(IncompleteSessionError.SESSION_RETRIEVAL_FAILED, any()) }
        coVerify(exactly = 0) { currentSessionCache.saveSession(any(), any()) }
    }

    @Test
    fun incVm_b04_multipleResumes_emitMultipleNavigateEvents_andSaveEachTime() = runTest {
        val sessionOne = makeSession(SessionType.DATA_COLLECTION)
        val sessionTwo = makeSession(SessionType.SURVEILLANCE)

        val sessionAndSiteOne = makeSessionAndSite(sessionOne, 11)
        val sessionAndSiteTwo = makeSessionAndSite(sessionTwo, 22)

        coEvery { sessionRepository.getSessionAndSiteById(sessionOne.localId) } returns sessionAndSiteOne
        coEvery { sessionRepository.getSessionAndSiteById(sessionTwo.localId) } returns sessionAndSiteTwo

        viewModel.events.test {
            viewModel.onAction(IncompleteSessionAction.ResumeSession(sessionOne.localId))
            assertThat(awaitItem()).isEqualTo(
                IncompleteSessionEvent.NavigateToIntakeScreen(SessionType.DATA_COLLECTION)
            )

            viewModel.onAction(IncompleteSessionAction.ResumeSession(sessionTwo.localId))
            assertThat(awaitItem()).isEqualTo(
                IncompleteSessionEvent.NavigateToIntakeScreen(SessionType.SURVEILLANCE)
            )

            expectNoEvents()
        }

        coVerify(exactly = 1) { currentSessionCache.saveSession(sessionOne, 11) }
        coVerify(exactly = 1) { currentSessionCache.saveSession(sessionTwo, 22) }
    }

    // ========================================
    // C. Return To Landing
    // ========================================

    @Test
    fun incVm_c01_returnToLanding_emitsNavigateBackEvent() = runTest {
        viewModel.events.test {
            viewModel.onAction(IncompleteSessionAction.ReturnToLandingScreen)
            val event = awaitItem()
            assertThat(event).isEqualTo(IncompleteSessionEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }
    }

    // ========================================
    // D. Edge Cases
    // ========================================

    @Test
    fun incVm_d01_resumeSession_withoutCollector_stillPerformsSideEffects() = runTest {
        val session = makeSession(SessionType.DATA_COLLECTION)
        val sessionAndSite = makeSessionAndSite(session, 77)
        coEvery { sessionRepository.getSessionAndSiteById(session.localId) } returns sessionAndSite

        viewModel.onAction(IncompleteSessionAction.ResumeSession(session.localId))
        advanceUntilIdle()

        coVerify(exactly = 1) { currentSessionCache.saveSession(session, 77) }
    }
}
