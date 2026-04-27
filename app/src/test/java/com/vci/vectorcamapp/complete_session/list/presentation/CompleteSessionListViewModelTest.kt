package com.vci.vectorcamapp.complete_session.list.presentation

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CompleteSessionListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var sessionRepository: SessionRepository
    private lateinit var specimenRepository: SpecimenRepository
    private lateinit var specimenImageRepository: SpecimenImageRepository
    private lateinit var workManagerRepository: WorkManagerRepository
    private lateinit var currentSessionCache: CurrentSessionCache
    private lateinit var errorMessageEmitter: ErrorMessageEmitter

    private lateinit var completeSessionsFlow: MutableStateFlow<List<SessionAndSite>>

    private val testSessionId = UUID.randomUUID()
    private val testSession = Session(
        localId = testSessionId,
        remoteId = null,
        hardwareId = "HW001",
        collectorTitle = "Dr.",
        collectorName = "Alice",
        collectorLastTrainedOn = 0L,
        collectionDate = 1_000_000L,
        collectionMethod = "Net",
        specimenCondition = "Good",
        createdAt = 1_000_100L,
        completedAt = 1_000_200L,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        type = SessionType.SURVEILLANCE,
    )
    private val testSite = Site(
        id = 1,
        district = "Test District",
        subCounty = "Test SubCounty",
        parish = "Test Parish",
        villageName = "Test Village",
        houseNumber = "123",
        healthCenter = "Test Center",
        isActive = true
    )

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        every { context.getString(any()) } returns ""

        sessionRepository = mockk(relaxed = true)
        specimenRepository = mockk(relaxed = true)
        specimenImageRepository = mockk(relaxed = true)
        workManagerRepository = mockk(relaxed = true)
        currentSessionCache = mockk(relaxed = true)
        coEvery { currentSessionCache.getSession() } returns null
        coEvery { currentSessionCache.getSiteId() } returns null
        errorMessageEmitter = mockk(relaxed = true)
        coEvery { errorMessageEmitter.emit(any(), any()) } returns Unit

        completeSessionsFlow = MutableStateFlow(emptyList())
        every { sessionRepository.observeCompleteSessionsAndSites() } returns completeSessionsFlow

        every { specimenImageRepository.observeUploadedMetadataCountForSession(any()) } returns flowOf(0)
        every { specimenImageRepository.observeUploadedImageCountForSession(any()) } returns flowOf(0)
        every { specimenImageRepository.observeFailedImageCountForSession(any()) } returns flowOf(0)
        every { workManagerRepository.observeIsSessionActivelyUploading(any()) } returns flowOf(false)
        coEvery { specimenImageRepository.getTotalCountForSession(any()) } returns 0
    }

    private fun makeViewModel() = CompleteSessionListViewModel(
        context = context,
        sessionRepository = sessionRepository,
        specimenRepository = specimenRepository,
        specimenImageRepository = specimenImageRepository,
        workManagerRepository = workManagerRepository,
        currentSessionCache = currentSessionCache,
        errorMessageEmitter = errorMessageEmitter,
    )

    // ========================================
    // A. Navigation Events
    // ========================================

    @Test
    fun listVm_a01_returnToLandingScreen_emitsEvent() = runTest {
        val vm = makeViewModel()

        vm.events.test {
            vm.onAction(CompleteSessionListAction.ReturnToLandingScreen)
            assertThat(awaitItem()).isEqualTo(CompleteSessionListEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }
    }

    @Test
    fun listVm_a02_viewCompleteSessionDetails_emitsEventWithCorrectId() = runTest {
        val vm = makeViewModel()
        val sessionId = UUID.randomUUID()

        vm.events.test {
            vm.onAction(CompleteSessionListAction.ViewCompleteSessionDetails(sessionId))
            val event = awaitItem()
            assertThat(event).isEqualTo(CompleteSessionListEvent.NavigateToCompleteSessionDetails(sessionId))
            expectNoEvents()
        }
    }

    // ========================================
    // B. Search Query
    // ========================================

    @Test
    fun listVm_b01_updateSearchQuery_updatesState() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(1) // initialValue

            vm.onAction(CompleteSessionListAction.UpdateSearchQuery("Alice"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.searchQuery).isEqualTo("Alice")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun listVm_b02_clearSearchQuery_resetsToEmpty() = runTest {
        val vm = makeViewModel()

        // Setting then clearing returns searchQuery to "" (the default) → deduplication → assert directly.
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(CompleteSessionListAction.UpdateSearchQuery("Alice"))
        vm.onAction(CompleteSessionListAction.UpdateSearchQuery(""))
        advanceUntilIdle()

        assertThat(vm.state.value.searchQuery).isEmpty()
    }

    // ========================================
    // C. Search Tooltip Visibility
    // ========================================

    @Test
    fun listVm_c01_showSearchTooltip_setsVisible() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(1)

            vm.onAction(CompleteSessionListAction.ShowSearchTooltipDialog)
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.isSearchTooltipVisible).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun listVm_c02_hideSearchTooltip_clearsVisible() = runTest {
        val vm = makeViewModel()

        // Show then hide returns isSearchTooltipVisible to false (its default) → deduplication → assert directly.
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(CompleteSessionListAction.ShowSearchTooltipDialog)
        vm.onAction(CompleteSessionListAction.HideSearchTooltipDialog)
        advanceUntilIdle()

        assertThat(vm.state.value.isSearchTooltipVisible).isFalse()
    }

    // ========================================
    // D. Upload All Pending Sessions
    // ========================================

    @Test
    fun listVm_d01_uploadAllPendingSessions_withEmptyList_doesNotEnqueue() = runTest {
        completeSessionsFlow.value = emptyList()
        val vm = makeViewModel()

        vm.onAction(CompleteSessionListAction.UploadAllPendingSessions)
        advanceUntilIdle()

        coVerify(exactly = 0) { workManagerRepository.enqueueSessionUpload(any(), any()) }
    }

    @Test
    fun listVm_d02_uploadAllPendingSessions_withAlreadySubmittedSessions_skipsThose() = runTest {
        val submittedSession = testSession.copy(submittedAt = 9_999_999L)
        completeSessionsFlow.value = listOf(SessionAndSite(submittedSession, testSite))
        coEvery { specimenRepository.getSpecimenImagesAndInferenceResultsBySession(any()) } returns
            emptyList<SpecimenWithSpecimenImagesAndInferenceResults>()

        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onAction(CompleteSessionListAction.UploadAllPendingSessions)
        advanceUntilIdle()

        coVerify(exactly = 0) { workManagerRepository.enqueueSessionUpload(any(), any()) }
    }

    @Test
    fun listVm_d03_uploadAllPendingSessions_withPendingSession_enqueuesUpload() = runTest {
        val pendingSession = testSession.copy(submittedAt = null)
        completeSessionsFlow.value = listOf(SessionAndSite(pendingSession, testSite))
        coEvery { specimenRepository.getSpecimenImagesAndInferenceResultsBySession(any()) } returns
            emptyList<SpecimenWithSpecimenImagesAndInferenceResults>()

        val vm = makeViewModel()
        advanceUntilIdle()

        vm.onAction(CompleteSessionListAction.UploadAllPendingSessions)
        advanceUntilIdle()

        coVerify(exactly = 1) { workManagerRepository.enqueueSessionUpload(testSessionId, testSite.id) }
    }

    // ========================================
    // E. State Updates from Flow
    // ========================================

    @Test
    fun listVm_e01_emptySessionsFlow_producesEmptyMap() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            val s = awaitItem()
            assertThat(s.sessionAndSiteToUploadProgress).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun listVm_e02_sessionsInFlow_producesMapWithProgress() = runTest {
        completeSessionsFlow.value = listOf(SessionAndSite(testSession, testSite))

        val vm = makeViewModel()

        vm.state.test {
            awaitItem() // may be initialValue or combined state
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.sessionAndSiteToUploadProgress).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
