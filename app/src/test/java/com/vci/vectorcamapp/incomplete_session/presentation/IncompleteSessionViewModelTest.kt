package com.vci.vectorcamapp.incomplete_session.presentation

import android.net.Uri
import android.util.Log
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
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class IncompleteSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionRepository: SessionRepository
    private lateinit var currentSessionCache: CurrentSessionCache
    private lateinit var cameraRepository: CameraRepository
    private lateinit var viewModel: IncompleteSessionViewModel

    private lateinit var incompleteSessionsFlow: MutableStateFlow<List<SessionAndSite>>

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        mockkStatic(Log::class)

        mockkObject(ErrorMessageBus)
        coEvery { ErrorMessageBus.emit(any(), any()) } returns Unit
        every { Uri.parse(any()) } answers { mockk(relaxed = true) }

        sessionRepository = mockk()
        currentSessionCache = mockk(relaxed = true)
        cameraRepository = mockk(relaxed = true)

        incompleteSessionsFlow = MutableStateFlow(emptyList())
        every { sessionRepository.observeIncompleteSessionsAndSites() } returns incompleteSessionsFlow

        viewModel = IncompleteSessionViewModel(
            sessionRepository = sessionRepository,
            currentSessionCache = currentSessionCache,
            cameraRepository = cameraRepository
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("android.net.Uri")
        unmockkStatic("android.util.Log")
        unmockkObject(ErrorMessageBus)
    }

    // ========================================
    // Test Harness / Helpers
    // ========================================

    private fun makeSession(sessionType: SessionType) = Session(
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
        type = sessionType,
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
            assertThat(initialState.sessionAndSites).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun incVm_a02_stateUpdatesWhenRepositoryEmits() = runTest {
        advanceUntilIdle()

        viewModel.state.test {
            awaitItem()

            val newSessionsAndSites = listOf(
                makeSessionAndSite(makeSession(SessionType.SURVEILLANCE), 0),
                makeSessionAndSite(makeSession(SessionType.DATA_COLLECTION), 1)
            )
            incompleteSessionsFlow.value = newSessionsAndSites
            advanceUntilIdle()

            val updatedState = awaitItem()
            assertThat(updatedState.sessionAndSites).isEqualTo(newSessionsAndSites)
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

    // ========================================
    // E. Delete Incomplete Session
    // ========================================

    @Test
    fun incVm_e01_deleteSession_setsDialogId() = runTest {
        val id = UUID.randomUUID()
        viewModel.state.test {
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.DeleteSession(id))
            val withDialog = awaitItem()
            assertThat(withDialog.deleteDialogSessionId).isEqualTo(id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun incVm_e02_dismissDeleteDialog_clearsDialogId() = runTest {
        val id = UUID.randomUUID()
        viewModel.state.test {
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.DeleteSession(id))
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.DismissDeleteDialog)
            val cleared = awaitItem()
            assertThat(cleared.deleteDialogSessionId).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun incVm_e03_confirmDelete_success_deletesAllImages_deletesSession_clearsDialog_noErrors() = runTest {
        val session = makeSession(SessionType.SURVEILLANCE)
        val siteId = 9
        val sessionAndSite = makeSessionAndSite(session, siteId)

        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        every { Uri.parse("file:///tmp/a.jpg") } returns uri1
        every { Uri.parse("file:///tmp/b.jpg") } returns uri2

        coEvery { sessionRepository.getSessionAndSiteById(session.localId) } returns sessionAndSite
        coEvery { sessionRepository.getImageUrisBySessionId(session.localId) } returns listOf(uri1, uri2)
        coEvery { cameraRepository.deleteSavedImage(uri1) } returns Unit
        coEvery { cameraRepository.deleteSavedImage(uri2) } returns Unit
        coEvery { sessionRepository.deleteSession(session, siteId) } returns true

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.DeleteSession(session.localId))
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.ConfirmDeleteSession(session.localId))
            val cleared = awaitItem()
            assertThat(cleared.deleteDialogSessionId).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { cameraRepository.deleteSavedImage(uri1) }
        coVerify(exactly = 1) { cameraRepository.deleteSavedImage(uri2) }
        coVerify(exactly = 1) { sessionRepository.deleteSession(session, siteId) }
    }

    @Test
    fun incVm_e04_confirmDelete_sessionNotFound_emitsError_doesNotDelete_clearsDialog() = runTest {
        val id = UUID.randomUUID()
        coEvery { sessionRepository.getSessionAndSiteById(id) } returns null

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.DeleteSession(id))
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.ConfirmDeleteSession(id))
            val cleared = awaitItem()
            assertThat(cleared.deleteDialogSessionId).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(IncompleteSessionError.SESSION_NOT_FOUND, any()) }
        coVerify(exactly = 0) { cameraRepository.deleteSavedImage(any()) }
        coVerify(exactly = 0) { sessionRepository.deleteSession(any(), any()) }
    }

    @Test
    fun incVm_e05_confirmDelete_deleteSessionReturnsFalse_emitsDeletionFailed_clearsDialog() = runTest {
        val session = makeSession(SessionType.DATA_COLLECTION)
        val siteId = 13
        val sessionAndSite = makeSessionAndSite(session, siteId)
        val uri = Uri.parse("file:///tmp/only.jpg")

        coEvery { sessionRepository.getSessionAndSiteById(session.localId) } returns sessionAndSite
        coEvery { sessionRepository.getImageUrisBySessionId(session.localId) } returns listOf(uri)
        coEvery { cameraRepository.deleteSavedImage(uri) } returns Unit
        coEvery { sessionRepository.deleteSession(session, siteId) } returns false

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.DeleteSession(session.localId))
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.ConfirmDeleteSession(session.localId))
            val cleared = awaitItem()
            assertThat(cleared.deleteDialogSessionId).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(IncompleteSessionError.SESSION_DELETION_FAILED, any()) }
        coVerify(exactly = 1) { cameraRepository.deleteSavedImage(uri) }
        coVerify(exactly = 1) { sessionRepository.deleteSession(session, siteId) }
    }

    @Test
    fun incVm_e06_confirmDelete_oneImageDeleteThrows_stillDeletesRest_andSession_clearsDialog_noErrors() = runTest {
        val session = makeSession(SessionType.SURVEILLANCE)
        val siteId = 21
        val sessionAndSite = makeSessionAndSite(session, siteId)

        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        every { Uri.parse("file:///tmp/a.jpg") } returns uri1
        every { Uri.parse("file:///tmp/b.jpg") } returns uri2

        coEvery { sessionRepository.getSessionAndSiteById(session.localId) } returns sessionAndSite
        coEvery { sessionRepository.getImageUrisBySessionId(session.localId) } returns listOf(uri1, uri2)
        coEvery { cameraRepository.deleteSavedImage(uri1) } throws RuntimeException("fail one")
        coEvery { cameraRepository.deleteSavedImage(uri2) } returns Unit
        coEvery { sessionRepository.deleteSession(session, siteId) } returns true

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.DeleteSession(session.localId))
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.ConfirmDeleteSession(session.localId))
            val cleared = awaitItem()
            assertThat(cleared.deleteDialogSessionId).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { cameraRepository.deleteSavedImage(uri1) }
        coVerify(exactly = 1) { cameraRepository.deleteSavedImage(uri2) }
        coVerify(exactly = 1) { sessionRepository.deleteSession(session, siteId) }
    }

    @Test
    fun incVm_e07_confirmDelete_repositoryThrows_emitsDeletionFailed_andClearsDialog() = runTest {
        val id = UUID.randomUUID()
        coEvery { sessionRepository.getSessionAndSiteById(id) } throws RuntimeException("db down")

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.DeleteSession(id))
            awaitItem()
            viewModel.onAction(IncompleteSessionAction.ConfirmDeleteSession(id))
            val cleared = awaitItem()
            assertThat(cleared.deleteDialogSessionId).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(IncompleteSessionError.SESSION_DELETION_FAILED, any()) }
        coVerify(exactly = 0) { cameraRepository.deleteSavedImage(any()) }
        coVerify(exactly = 0) { sessionRepository.deleteSession(any(), any()) }
    }
}
