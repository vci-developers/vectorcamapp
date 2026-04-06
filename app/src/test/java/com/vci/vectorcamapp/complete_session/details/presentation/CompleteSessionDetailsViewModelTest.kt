package com.vci.vectorcamapp.complete_session.details.presentation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.complete_session.details.domain.util.CompleteSessionDetailsError
import com.vci.vectorcamapp.complete_session.details.presentation.enums.CompleteSessionDetailsTab
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CompleteSessionDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var sessionRepository: SessionRepository
    private lateinit var specimenRepository: SpecimenRepository
    private lateinit var errorMessageEmitter: ErrorMessageEmitter

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
        completedAt = null,
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
        errorMessageEmitter = mockk(relaxed = true)
        coEvery { errorMessageEmitter.emit(any(), any()) } returns Unit

        every { specimenRepository.observeSpecimenImagesAndInferenceResultsBySession(any()) } returns
            MutableStateFlow(emptyList())
    }

    private fun makeViewModel(
        sessionIdInHandle: String? = testSessionId.toString(),
        sessionAndSite: SessionAndSite? = SessionAndSite(testSession, testSite),
        surveillanceFormResult: com.vci.vectorcamapp.core.domain.model.composites.SessionAndSurveillanceForm? = null
    ): CompleteSessionDetailsViewModel {
        val savedStateHandle = SavedStateHandle(
            if (sessionIdInHandle != null) mapOf("sessionId" to sessionIdInHandle) else emptyMap()
        )
        coEvery { sessionRepository.getSessionAndSiteById(any()) } returns sessionAndSite
        coEvery { sessionRepository.getSessionAndSurveillanceFormById(any()) } returns surveillanceFormResult

        return CompleteSessionDetailsViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            sessionRepository = sessionRepository,
            specimenRepository = specimenRepository,
            errorMessageEmitter = errorMessageEmitter,
        )
    }

    // ========================================
    // A. Initialization & Loading
    // ========================================

    @Test
    fun detailsVm_a01_noSessionIdInHandle_emitsError() = runTest {
        val vm = makeViewModel(sessionIdInHandle = null)

        // awaitItem() alone only receives the stateIn initial value — loadCompleteSessionDetails
        // is still pending. advanceUntilIdle() lets it run before the coVerify check.
        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(atLeast = 1) { errorMessageEmitter.emit(CompleteSessionDetailsError.SESSION_NOT_FOUND, any()) }
    }

    @Test
    fun detailsVm_a02_sessionNotFound_emitsSessionNotFoundError() = runTest {
        val vm = makeViewModel(sessionAndSite = null)

        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(atLeast = 1) { errorMessageEmitter.emit(CompleteSessionDetailsError.SESSION_NOT_FOUND, any()) }
    }

    @Test
    fun detailsVm_a03_getSessionAndSiteReturnsNull_emitsSessionNotFoundError() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("sessionId" to testSessionId.toString()))
        coEvery { sessionRepository.getSessionAndSiteById(any()) } returns null
        coEvery { sessionRepository.getSessionAndSurveillanceFormById(any()) } returns null

        val vm = CompleteSessionDetailsViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            sessionRepository = sessionRepository,
            specimenRepository = specimenRepository,
            errorMessageEmitter = errorMessageEmitter,
        )

        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(atLeast = 1) { errorMessageEmitter.emit(CompleteSessionDetailsError.SESSION_NOT_FOUND, any()) }
    }

    @Test
    fun detailsVm_a04_validSession_populatesStateWithSessionAndSite() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            awaitItem() // triggers onStart
            val loaded = awaitItem() // after loadCompleteSessionDetails sets session + site
            assertThat(loaded.session).isEqualTo(testSession)
            assertThat(loaded.site).isEqualTo(testSite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // B. Navigation Events
    // ========================================

    @Test
    fun detailsVm_b01_returnToCompleteSessionList_emitsEvent() = runTest {
        val vm = makeViewModel()

        vm.events.test {
            vm.onAction(CompleteSessionDetailsAction.ReturnToCompleteSessionListScreen)
            assertThat(awaitItem()).isEqualTo(CompleteSessionDetailsEvent.NavigateBackToCompleteSessionListScreen)
            expectNoEvents()
        }
    }

    // ========================================
    // C. Tab Selection
    // ========================================

    @Test
    fun detailsVm_c01_changeTab_updatesSelectedTab() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(2) // initialValue + after loadCompleteSessionDetails

            vm.onAction(CompleteSessionDetailsAction.ChangeSelectedTab(CompleteSessionDetailsTab.SESSION_SPECIMENS))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.selectedTab).isEqualTo(CompleteSessionDetailsTab.SESSION_SPECIMENS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun detailsVm_c02_changeTabBack_updatesSelectedTab() = runTest {
        val vm = makeViewModel()

        // The round-trip (SPECIMENS → FORM) returns selectedTab to its default (SESSION_FORM),
        // so StateFlow deduplicates and emits nothing new. Assert state.value directly.
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(CompleteSessionDetailsAction.ChangeSelectedTab(CompleteSessionDetailsTab.SESSION_SPECIMENS))
        vm.onAction(CompleteSessionDetailsAction.ChangeSelectedTab(CompleteSessionDetailsTab.SESSION_FORM))
        advanceUntilIdle()

        assertThat(vm.state.value.selectedTab).isEqualTo(CompleteSessionDetailsTab.SESSION_FORM)
    }

    // ========================================
    // D. Search Query
    // ========================================

    @Test
    fun detailsVm_d01_updateSearchQuery_updatesState() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(2)

            vm.onAction(CompleteSessionDetailsAction.UpdateSearchQuery("ABC"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.searchQuery).isEqualTo("ABC")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun detailsVm_d02_clearSearchQuery_emptiesQuery() = runTest {
        val vm = makeViewModel()

        // Setting then clearing the query returns to "" (the default) → deduplication → assert directly.
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(CompleteSessionDetailsAction.UpdateSearchQuery("ABC"))
        vm.onAction(CompleteSessionDetailsAction.UpdateSearchQuery(""))
        advanceUntilIdle()

        assertThat(vm.state.value.searchQuery).isEmpty()
    }

    // ========================================
    // E. Search Tooltip
    // ========================================

    @Test
    fun detailsVm_e01_showSearchTooltip_setsVisible() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(2)

            vm.onAction(CompleteSessionDetailsAction.ShowSearchTooltipDialog)
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.isSearchTooltipVisible).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun detailsVm_e02_hideSearchTooltip_clearsVisible() = runTest {
        val vm = makeViewModel()

        // Show then hide returns isSearchTooltipVisible to false (its default) → deduplication → assert directly.
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(CompleteSessionDetailsAction.ShowSearchTooltipDialog)
        vm.onAction(CompleteSessionDetailsAction.HideSearchTooltipDialog)
        advanceUntilIdle()

        assertThat(vm.state.value.isSearchTooltipVisible).isFalse()
    }
}
