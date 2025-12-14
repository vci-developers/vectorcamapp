package com.vci.vectorcamapp.complete_session.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.complete_session.details.domain.util.CompleteSessionDetailsError
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsAction
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsEvent
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsViewModel
import com.vci.vectorcamapp.complete_session.details.presentation.enums.CompleteSessionDetailsTab
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSurveillanceForm
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenImageAndInferenceResult
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageBus
import com.vci.vectorcamapp.core.presentation.util.search.SearchUtils
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CompleteSessionDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var sessionRepository: SessionRepository
    private lateinit var specimenRepository: SpecimenRepository
    private lateinit var viewModel: CompleteSessionDetailsViewModel

    private lateinit var specimensFlow: MutableStateFlow<List<SpecimenWithSpecimenImagesAndInferenceResults>>

    private val testSessionId = UUID.randomUUID()

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        mockkStatic(Log::class)
        mockkStatic(SearchUtils::class)
        mockkObject(SearchUtils)
        mockkObject(ErrorMessageBus)
        coEvery { ErrorMessageBus.emit(any(), any()) } returns Unit
        every { Uri.parse(any()) } answers { mockk(relaxed = true) }
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        every { SearchUtils.matchesQuery(any(), any<List<String?>>()) } returns true

        context = mockk(relaxed = true)
        every { context.getString(any()) } returns "MockString"

        sessionRepository = mockk()
        specimenRepository = mockk()

        savedStateHandle = SavedStateHandle().apply {
            set("sessionId", testSessionId.toString())
        }

        specimensFlow = MutableStateFlow(emptyList())
        every { specimenRepository.observeSpecimenImagesAndInferenceResultsBySession(any()) } returns specimensFlow

        val session = makeSession()
        val site = makeSite()
        coEvery { sessionRepository.getSessionAndSiteById(testSessionId) } returns SessionAndSite(session, site)
        coEvery { sessionRepository.getSessionAndSurveillanceFormById(testSessionId) } returns SessionAndSurveillanceForm(session, null)

        viewModel = CompleteSessionDetailsViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            sessionRepository = sessionRepository,
            specimenRepository = specimenRepository
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("android.net.Uri")
        unmockkStatic("android.util.Log")
        unmockkStatic(SearchUtils::class)
        unmockkObject(ErrorMessageBus)
    }

    // ========================================
    // Test Harness / Helpers
    // ========================================

    private fun makeSession() = Session(
        localId = testSessionId,
        remoteId = 123,
        hardwareId = "HW-TEST",
        collectorTitle = "Mr.",
        collectorName = "John",
        collectorLastTrainedOn = 0L,
        collectionDate = 1000L,
        collectionMethod = "Trap",
        specimenCondition = "Good",
        createdAt = 1000L,
        completedAt = 2000L,
        submittedAt = null,
        notes = "Notes",
        latitude = 1.0f,
        longitude = 1.0f,
        type = SessionType.SURVEILLANCE
    )

    private fun makeSite() = Site(
        id = 1,
        district = "D1",
        subCounty = "SC1",
        parish = "P1",
        villageName = "V1",
        houseNumber = "H1",
        healthCenter = "HC1",
        isActive = true
    )

    private fun makeSpecimenWithImage(id: String): SpecimenWithSpecimenImagesAndInferenceResults {
        val specimen = Specimen(id = id, remoteId = null, shouldProcessFurther = true)
        val image = SpecimenImage(
            localId = UUID.randomUUID().toString(),
            remoteId = null,
            species = "Anopheles",
            sex = "Female",
            abdomenStatus = "Fed",
            imageUri = Uri.parse("file://test"),
            metadataUploadStatus = UploadStatus.COMPLETED,
            imageUploadStatus = UploadStatus.COMPLETED,
            capturedAt = 1000L,
            submittedAt = null
        )
        return SpecimenWithSpecimenImagesAndInferenceResults(
            specimen = specimen,
            specimenImagesAndInferenceResults = listOf(
                SpecimenImageAndInferenceResult(image, null)
            )
        )
    }

    // ========================================
    // A. Initialization & Loading
    // ========================================

    @Test
    fun cplVm_a01_initialization_loadsSessionSiteAndSpecimens() = runTest {
        val specimens = listOf(makeSpecimenWithImage("SPEC-1"))
        specimensFlow.value = specimens

        viewModel.state.test {
            awaitItem()

            advanceUntilIdle()

            val loadedState = awaitItem()

            assertThat(loadedState.session.localId).isEqualTo(testSessionId)
            assertThat(loadedState.site.district).isEqualTo("D1")

            assertThat(loadedState.specimensWithImagesAndInferenceResults).hasSize(1)
            assertThat(loadedState.specimensWithImagesAndInferenceResults.first().specimen.id).isEqualTo("SPEC-1")

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { sessionRepository.getSessionAndSiteById(testSessionId) }
    }

    @Test
    fun cplVm_a02_initialization_missingSessionId_emitsError() = runTest {
        savedStateHandle = SavedStateHandle()
        viewModel = CompleteSessionDetailsViewModel(
            context,
            savedStateHandle,
            sessionRepository,
            specimenRepository
        )

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(CompleteSessionDetailsError.SESSION_NOT_FOUND, any()) }
    }

    @Test
    fun cplVm_a03_initialization_sessionNotFoundInRepo_emitsError() = runTest {
        coEvery { sessionRepository.getSessionAndSiteById(testSessionId) } returns null

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(CompleteSessionDetailsError.SESSION_NOT_FOUND, any()) }
    }

    @Test
    fun cplVm_a04_initialization_siteNotFoundInRepo_emitsError() = runTest {
        val validSession = makeSession()

        val mockComposite = mockk<SessionAndSite>()
        every { mockComposite.session } returns validSession
        every { mockComposite.site } answers { unsafeNull() }

        coEvery { sessionRepository.getSessionAndSiteById(testSessionId) } returns mockComposite

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(CompleteSessionDetailsError.SITE_NOT_FOUND, any()) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> unsafeNull(): T = null as T

    // ========================================
    // B. Search & Filtering
    // ========================================

    @Test
    fun cplVm_b01_searchQuery_updatesState_andFiltersSpecimens() = runTest {
        val s1 = makeSpecimenWithImage("MATCH")
        val s2 = makeSpecimenWithImage("NOPE")
        specimensFlow.value = listOf(s1, s2)

        every { SearchUtils.matchesQuery(eq("query"), any<List<String?>>()) } answers {
            val fields = secondArg<List<String?>>()
            fields.contains("MATCH")
        }

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            awaitItem()

            viewModel.onAction(CompleteSessionDetailsAction.UpdateSearchQuery("query"))

            val searchState = awaitItem()
            assertThat(searchState.searchQuery).isEqualTo("query")
            assertThat(searchState.specimensWithImagesAndInferenceResults).hasSize(1)
            assertThat(searchState.specimensWithImagesAndInferenceResults.first().specimen.id).isEqualTo("MATCH")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cplVm_b02_emptySearchQuery_showsAllSpecimens() = runTest {
        val s1 = makeSpecimenWithImage("A")
        val s2 = makeSpecimenWithImage("B")
        specimensFlow.value = listOf(s1, s2)

        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            val loaded = awaitItem()
            assertThat(loaded.specimensWithImagesAndInferenceResults).hasSize(2)

            viewModel.onAction(CompleteSessionDetailsAction.UpdateSearchQuery("   "))
            val blankState = awaitItem()
            assertThat(blankState.searchQuery).isEqualTo("   ")
            assertThat(blankState.specimensWithImagesAndInferenceResults).hasSize(2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // C. Tabs & UI State
    // ========================================

    @Test
    fun cplVm_c01_changeSelectedTab_updatesState() = runTest {
        viewModel.state.test {
            val initial = awaitItem()
            assertThat(initial.selectedTab).isEqualTo(CompleteSessionDetailsTab.SESSION_FORM)

            viewModel.onAction(CompleteSessionDetailsAction.ChangeSelectedTab(CompleteSessionDetailsTab.SESSION_SPECIMENS))

            val updated = awaitItem()
            assertThat(updated.selectedTab).isEqualTo(CompleteSessionDetailsTab.SESSION_SPECIMENS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cplVm_c02_tooltipDialogs_updateState() = runTest {
        viewModel.state.test {
            val initial = awaitItem()
            assertThat(initial.isSearchTooltipVisible).isFalse()

            viewModel.onAction(CompleteSessionDetailsAction.ShowSearchTooltipDialog)
            assertThat(awaitItem().isSearchTooltipVisible).isTrue()

            viewModel.onAction(CompleteSessionDetailsAction.HideSearchTooltipDialog)
            assertThat(awaitItem().isSearchTooltipVisible).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // D. Navigation
    // ========================================

    @Test
    fun cplVm_d01_returnToList_emitsNavigateEvent() = runTest {
        viewModel.events.test {
            viewModel.onAction(CompleteSessionDetailsAction.ReturnToCompleteSessionListScreen)
            val event = awaitItem()
            assertThat(event).isEqualTo(CompleteSessionDetailsEvent.NavigateBackToCompleteSessionListScreen)
            expectNoEvents()
        }
    }
}
