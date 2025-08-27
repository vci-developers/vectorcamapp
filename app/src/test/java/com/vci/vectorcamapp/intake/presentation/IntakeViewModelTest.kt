package com.vci.vectorcamapp.intake.presentation

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.repository.SurveillanceFormRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageBus
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.intake.domain.repository.LocationRepository
import com.vci.vectorcamapp.intake.domain.strategy.SurveillanceFormWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.SurveillanceFormWorkflowFactory
import com.vci.vectorcamapp.intake.domain.use_cases.ValidationUseCases
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntakeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var validationUseCases: ValidationUseCases
    private lateinit var deviceCache: DeviceCache
    private lateinit var currentSessionCache: CurrentSessionCache
    private lateinit var siteRepository: SiteRepository
    private lateinit var surveillanceFormRepository: SurveillanceFormRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var transactionHelper: TransactionHelper
    private lateinit var surveillanceFormWorkflowFactory: SurveillanceFormWorkflowFactory
    private lateinit var surveillanceFormWorkflow: SurveillanceFormWorkflow

    private lateinit var intakeViewModel: IntakeViewModel

    private fun makeSession(sessionType: SessionType) = Session(
        localId = java.util.UUID.randomUUID(),
        remoteId = null,
        houseNumber = "",
        collectorTitle = "",
        collectorName = "",
        collectionDate = 0L,
        collectionMethod = "",
        specimenCondition = "",
        createdAt = 1_700_000_000L,
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        type = sessionType
    )

    @Before
    fun setUp() {
        mockkObject(ErrorMessageBus)
        coEvery { ErrorMessageBus.emit(any(), any()) } returns Unit

        savedStateHandle = SavedStateHandle(mapOf("sessionType" to SessionType.SURVEILLANCE))

        validationUseCases = mockk(relaxed = true)
        deviceCache = mockk(relaxed = true)
        currentSessionCache = mockk(relaxed = true)
        siteRepository = mockk()
        surveillanceFormRepository = mockk()
        sessionRepository = mockk()
        locationRepository = mockk(relaxed = true)

        coEvery { validationUseCases.validateCollectorTitle(any()) } returns Result.Success(Unit)
        coEvery { validationUseCases.validateCollectorName(any()) } returns Result.Success(Unit)
        coEvery { validationUseCases.validateDistrict(any()) } returns Result.Success(Unit)
        coEvery { validationUseCases.validateSentinelSite(any()) } returns Result.Success(Unit)
        coEvery { validationUseCases.validateHouseNumber(any()) } returns Result.Success(Unit)
        coEvery { validationUseCases.validateLlinType(any()) } returns Result.Success(Unit)
        coEvery { validationUseCases.validateLlinBrand(any()) } returns Result.Success(Unit)
        coEvery { validationUseCases.validateCollectionDate(any()) } returns Result.Success(Unit)
        coEvery { validationUseCases.validateCollectionMethod(any()) } returns Result.Success(Unit)
        coEvery { validationUseCases.validateSpecimenCondition(any()) } returns Result.Success(Unit)

        transactionHelper = mockk()
        coEvery { transactionHelper.runAsTransaction(any<suspend () -> Boolean>()) } coAnswers {
            val block = firstArg<suspend () -> Boolean>()
            block()
        }

        surveillanceFormWorkflowFactory = mockk()
        surveillanceFormWorkflow = mockk()
        every { surveillanceFormWorkflowFactory.create(any()) } returns surveillanceFormWorkflow
        val defaultSurveillanceForm: SurveillanceForm = mockk(relaxed = true)
        every { surveillanceFormWorkflow.getSurveillanceForm() } returns defaultSurveillanceForm
    }

    @After
    fun tearDown() {
        unmockkObject(ErrorMessageBus)
    }

    private fun initializeIntakeViewModel(
        programIdentifier: Int? = 1,
        existingSession: Session? = null,
        allSites: List<Site> = emptyList(),
        savedFormForExistingSession: SurveillanceForm? = null,
        siteIdentifierForExistingSession: Int? = null,
        initialLocationResult: Result<Location, IntakeError>? = null
    ) {
        coEvery { deviceCache.getProgramId() } returns programIdentifier
        coEvery { currentSessionCache.getSession() } returns existingSession
        if (siteIdentifierForExistingSession != null) {
            coEvery { currentSessionCache.getSiteId() } returns siteIdentifierForExistingSession
        }

        coEvery { siteRepository.getAllSitesByProgramId(any()) } returns allSites
        if (existingSession != null) {
            coEvery {
                surveillanceFormRepository.getSurveillanceFormBySessionId(existingSession.localId)
            } returns savedFormForExistingSession
        }

        if (initialLocationResult != null) {
            coEvery { locationRepository.getCurrentLocation() } returns initialLocationResult
        }


        intakeViewModel = IntakeViewModel(
            savedStateHandle = savedStateHandle,
            validationUseCases = validationUseCases,
            deviceCache = deviceCache,
            currentSessionCache = currentSessionCache,
            siteRepository = siteRepository,
            surveillanceFormRepository = surveillanceFormRepository,
            sessionRepository = sessionRepository,
            locationRepository = locationRepository
        )

        intakeViewModel.transactionHelper = transactionHelper
        intakeViewModel.surveillanceFormWorkflowFactory = surveillanceFormWorkflowFactory
    }

    // ========================================
    // A. Initialization
    // ========================================

    @Test
    fun intakeInitialization_a01_noProgram_sendsNavigateBackAndStopsLoading_andEmitsError() = runTest {
        initializeIntakeViewModel(programIdentifier = null)

        intakeViewModel.state.test {
            awaitItem()

            intakeViewModel.events.test {
                assertThat(awaitItem()).isEqualTo(IntakeEvent.NavigateBackToRegistrationScreen)
                expectNoEvents()
            }

            val afterLoad = awaitItem()
            assertThat(afterLoad.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(IntakeError.PROGRAM_NOT_FOUND, any()) }
    }

    @Test
    fun intakeInitialization_a02_validProgram_noExistingSession_setsDefaults_andCallsWorkflowFactory() = runTest {
        val siteOne: Site = mockk {
            every { id } returns 11
            every { district } returns "District A"
            every { sentinelSite } returns "Alpha"
        }

        val location: Location = mockk(relaxed = true) {
            every { latitude } returns 0.0
            every { longitude } returns 0.0
        }
        coEvery { locationRepository.getCurrentLocation() } returns Result.Success(location)

        initializeIntakeViewModel(
            programIdentifier = 7,
            existingSession = null,
            allSites = listOf(siteOne)
        )

        intakeViewModel.state.test {
            var loaded: IntakeState
            do {
                loaded = awaitItem()
            } while (loaded.allSitesInProgram.isEmpty())

            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.allSitesInProgram).hasSize(1)
            assertThat(loaded.selectedDistrict).isEqualTo("")
            assertThat(loaded.selectedSentinelSite).isEqualTo("")
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { ErrorMessageBus.emit(any(), any()) }
        coVerify(exactly = 1) { locationRepository.getCurrentLocation() }
    }

    @Test
    fun intakeInitialization_a03_existingSession_populatesSelectedDistrictAndSite_andLoadsSavedFormIfPresent() = runTest {
        val existingSession = makeSession(SessionType.DATA_COLLECTION)
        val targetSite: Site = mockk {
            every { id } returns 99
            every { district } returns "District Z"
            every { sentinelSite } returns "Omega"
        }
        val unrelatedSite: Site = mockk {
            every { id } returns 100
            every { district } returns "District A"
            every { sentinelSite } returns "Alpha"
        }
        val savedSurveillanceForm: SurveillanceForm = mockk(relaxed = true)

        val location: Location = mockk(relaxed = true) {
            every { latitude } returns 0.0
            every { longitude } returns 0.0
        }
        coEvery { locationRepository.getCurrentLocation() } returns Result.Success(location)

        initializeIntakeViewModel(
            programIdentifier = 1,
            existingSession = existingSession,
            allSites = listOf(unrelatedSite, targetSite),
            savedFormForExistingSession = savedSurveillanceForm,
            siteIdentifierForExistingSession = 99
        )

        intakeViewModel.state.test {
            var loaded: IntakeState
            do {
                loaded = awaitItem()
            } while (
                loaded.selectedDistrict.isEmpty() ||
                loaded.selectedSentinelSite.isEmpty() ||
                loaded.surveillanceForm == null
            )

            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.selectedDistrict).isEqualTo("District Z")
            assertThat(loaded.selectedSentinelSite).isEqualTo("Omega")
            assertThat(loaded.surveillanceForm).isNotNull()

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { locationRepository.getCurrentLocation() }
    }

    // ========================================
    // B. Navigation And Simple Actions
    // ========================================

    @Test
    fun intakeActions_b01_returnToLanding_clearsSession_andEmitsNavigateBack() = runTest {
        initializeIntakeViewModel(programIdentifier = 1)

        intakeViewModel.events.test {
            intakeViewModel.onAction(IntakeAction.ReturnToLandingScreen)
            assertThat(awaitItem()).isEqualTo(IntakeEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }

        coVerify(exactly = 1) { currentSessionCache.clearSession() }
    }

    @Test
    fun intakeActions_b02_selectDistrict_resetsSelectedSentinelSite() = runTest {
        val site: Site = mockk {
            every { id } returns 1
            every { district } returns "District A"
            every { sentinelSite } returns "Alpha"
        }

        val location: Location = mockk(relaxed = true) {
            every { latitude } returns 0.0
            every { longitude } returns 0.0
        }
        coEvery { locationRepository.getCurrentLocation() } returns Result.Success(location)

        initializeIntakeViewModel(programIdentifier = 1, allSites = listOf(site))

        intakeViewModel.state.test {
            var isLoaded: IntakeState
            do {
                isLoaded = awaitItem()
            } while (isLoaded.allSitesInProgram.isEmpty())

            intakeViewModel.onAction(IntakeAction.SelectDistrict("District A"))
            val afterDistrictA = awaitItem()
            assertThat(afterDistrictA.selectedDistrict).isEqualTo("District A")

            intakeViewModel.onAction(IntakeAction.SelectSentinelSite("Alpha"))
            val afterSiteSelect = awaitItem()
            assertThat(afterSiteSelect.selectedSentinelSite).isEqualTo("Alpha")

            intakeViewModel.onAction(IntakeAction.SelectDistrict("District B"))
            val afterDistrictB = awaitItem()
            assertThat(afterDistrictB.selectedDistrict).isEqualTo("District B")
            assertThat(afterDistrictB.selectedSentinelSite).isEqualTo("")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // C. Submit Flow — Success And Errors
    // ========================================

    @Test
    fun intakeSubmit_c01_successfulSubmit_runsTransaction_savesSession_andNavigatesToImaging() = runTest {
        val chosenSite: Site = mockk {
            every { id } returns 55
            every { district } returns "Kampala"
            every { sentinelSite } returns "Sentinel One"
        }

        val location: Location = mockk(relaxed = true) {
            every { latitude } returns 0.0
            every { longitude } returns 0.0
        }
        coEvery { locationRepository.getCurrentLocation() } returns Result.Success(location)

        initializeIntakeViewModel(
            programIdentifier = 1,
            allSites = listOf(chosenSite),
            existingSession = null
        )

        coEvery { sessionRepository.upsertSession(any(), 55) } returns Result.Success(Unit)
        coEvery { surveillanceFormRepository.upsertSurveillanceForm(any(), any()) } returns Result.Success(Unit)

        intakeViewModel.state.test {
            var state: IntakeState
            do { state = awaitItem() } while (state.allSitesInProgram.isEmpty())

            intakeViewModel.onAction(IntakeAction.SelectDistrict("Kampala"))
            awaitItem()
            intakeViewModel.onAction(IntakeAction.SelectSentinelSite("Sentinel One"))
            awaitItem()

            cancelAndIgnoreRemainingEvents()
        }

        intakeViewModel.events.test {
            intakeViewModel.onAction(IntakeAction.SubmitIntakeForm)
            assertThat(awaitItem()).isEqualTo(IntakeEvent.NavigateToImagingScreen)
            expectNoEvents()
        }

        coVerifyOrder {
            sessionRepository.upsertSession(any(), 55)
            surveillanceFormRepository.upsertSurveillanceForm(any(), any())
        }
        coVerify(exactly = 1) { currentSessionCache.saveSession(any(), 55) }
        coVerify(exactly = 0) { ErrorMessageBus.emit(any(), any()) }
    }

    @Test
    fun intakeSubmit_c02_missingSite_emitsError_andDoesNotRunTransaction() = runTest {
        initializeIntakeViewModel(programIdentifier = 1, allSites = emptyList())

        intakeViewModel.onAction(IntakeAction.SelectDistrict("Gulu"))
        intakeViewModel.onAction(IntakeAction.SelectSentinelSite("Nonexistent"))

        intakeViewModel.onAction(IntakeAction.SubmitIntakeForm)
        advanceUntilIdle()

        coVerify(exactly = 1) { ErrorMessageBus.emit(IntakeError.SITE_NOT_FOUND, any()) }
        coVerify { sessionRepository wasNot Called }
        coVerify { surveillanceFormRepository wasNot Called }
        coVerify { currentSessionCache wasNot Called }
    }

    @Test
    fun intakeSubmit_c03_validationFailure_preventsTransaction_andNoNavigation() = runTest {
        val site: Site = mockk {
            every { id } returns 9
            every { district } returns "Arua"
            every { sentinelSite } returns "Block A"
        }
        initializeIntakeViewModel(programIdentifier = 1, allSites = listOf(site))

        intakeViewModel.onAction(IntakeAction.SelectDistrict("Arua"))
        intakeViewModel.onAction(IntakeAction.SelectSentinelSite("Block A"))

        coEvery { validationUseCases.validateCollectorName(any()) } returns
                Result.Error(FormValidationError.BLANK_COLLECTOR_NAME)

        intakeViewModel.events.test {
            intakeViewModel.onAction(IntakeAction.SubmitIntakeForm)
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify { sessionRepository wasNot Called }
        coVerify { surveillanceFormRepository wasNot Called }
    }

    @Test
    fun intakeSubmit_c04_upsertSessionError_emitsError_andAborts() = runTest {
        val site: Site = mockk {
            every { id } returns 101
            every { district } returns "Jinja"
            every { sentinelSite } returns "Block B"
        }

        val location: Location = mockk(relaxed = true) {
            every { latitude } returns 0.0
            every { longitude } returns 0.0
        }
        coEvery { locationRepository.getCurrentLocation() } returns Result.Success(location)

        initializeIntakeViewModel(programIdentifier = 1, allSites = listOf(site))

        coEvery { sessionRepository.upsertSession(any(), 101) } returns
                Result.Error(RoomDbError.UNKNOWN_ERROR)

        intakeViewModel.state.test {
            var s: IntakeState
            do { s = awaitItem() } while (s.allSitesInProgram.isEmpty())

            intakeViewModel.onAction(IntakeAction.SelectDistrict("Jinja"))
            awaitItem()
            intakeViewModel.onAction(IntakeAction.SelectSentinelSite("Block B"))
            awaitItem()

            intakeViewModel.onAction(IntakeAction.SubmitIntakeForm)
            cancelAndIgnoreRemainingEvents()
        }

        advanceUntilIdle()

        coVerify(exactly = 1) { sessionRepository.upsertSession(any(), 101) }
        coVerify(exactly = 0) { surveillanceFormRepository.upsertSurveillanceForm(any(), any()) }

        coVerify(exactly = 1) { ErrorMessageBus.emit(RoomDbError.UNKNOWN_ERROR, any()) }
        coVerify(exactly = 0) { currentSessionCache.saveSession(any(), any()) }
    }

    @Test
    fun intakeSubmit_c05_upsertSurveillanceFormError_emitsError_andAborts() = runTest {
        val site: Site = mockk {
            every { id } returns 202
            every { district } returns "Lira"
            every { sentinelSite } returns "Center"
        }

        val location: Location = mockk(relaxed = true) {
            every { latitude } returns 0.0
            every { longitude } returns 0.0
        }
        coEvery { locationRepository.getCurrentLocation() } returns Result.Success(location)

        initializeIntakeViewModel(programIdentifier = 1, allSites = listOf(site))

        coEvery { sessionRepository.upsertSession(any(), 202) } returns Result.Success(Unit)
        coEvery { surveillanceFormRepository.upsertSurveillanceForm(any(), any()) } returns
                Result.Error(RoomDbError.UNKNOWN_ERROR)

        intakeViewModel.state.test {
            var s: IntakeState
            do { s = awaitItem() } while (s.allSitesInProgram.isEmpty())

            intakeViewModel.onAction(IntakeAction.SelectDistrict("Lira"))
            awaitItem()
            intakeViewModel.onAction(IntakeAction.SelectSentinelSite("Center"))
            awaitItem()

            intakeViewModel.onAction(IntakeAction.SubmitIntakeForm)

            cancelAndIgnoreRemainingEvents()
        }

        advanceUntilIdle()

        coVerify(exactly = 1) { sessionRepository.upsertSession(any(), 202) }
        coVerify(exactly = 1) { surveillanceFormRepository.upsertSurveillanceForm(any(), any()) }
        coVerify(exactly = 1) { ErrorMessageBus.emit(RoomDbError.UNKNOWN_ERROR, any()) } // use 3-arg if your bus overload requires it
        coVerify(exactly = 0) { currentSessionCache.saveSession(any(), any()) }
    }

    // ========================================
    // D. Location Behavior
    // ========================================
    @Test
    fun intakeLocation_d01_successOnInit_setsLatLng_andDoesNotSetError() = runTest {
        val sessionWithoutLocation = makeSession(SessionType.SURVEILLANCE).copy(
            latitude = null, longitude = null
        )
        val location: Location = mockk(relaxed = true) {
            every { latitude } returns 1.2345
            every { longitude } returns 6.7890
        }

        initializeIntakeViewModel(
            programIdentifier = 1,
            existingSession = sessionWithoutLocation,
            initialLocationResult = Result.Success(location)
        )

        intakeViewModel.state.test {
            var loaded: IntakeState
            do {
                loaded = awaitItem()
            } while (loaded.session.latitude == null || loaded.session.longitude == null)

            assertThat(loaded.locationError).isNull()
            assertThat(loaded.session.latitude).isEqualTo(1.2345f)
            assertThat(loaded.session.longitude).isEqualTo(6.7890f)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { locationRepository.getCurrentLocation() }
    }

    @Test
    fun intakeLocation_d02_existingSessionAlreadyHasLocation_skipsFetch() = runTest {
        val sessionWithLocation = makeSession(SessionType.SURVEILLANCE).copy(
            latitude = 0.55f,
            longitude = 32.60f
        )

        initializeIntakeViewModel(
            programIdentifier = 1,
            existingSession = sessionWithLocation
        )

        intakeViewModel.state.test {
            var loaded: IntakeState
            do {
                loaded = awaitItem()
            } while (loaded.session.latitude == null || loaded.session.longitude == null)

            assertThat(loaded.locationError).isNull()
            assertThat(loaded.session.latitude).isEqualTo(0.55f)
            assertThat(loaded.session.longitude).isEqualTo(32.60f)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify { locationRepository wasNot Called }
    }

    @Test
    fun intakeLocation_d03_repoReturnsPermissionDenied_emitsError_busOnly() = runTest {
        initializeIntakeViewModel(
            programIdentifier = 1,
            initialLocationResult = Result.Error(IntakeError.LOCATION_PERMISSION_DENIED)
        )

        intakeViewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.locationError).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(IntakeError.LOCATION_PERMISSION_DENIED, any()) }
        coVerify(exactly = 1) { locationRepository.getCurrentLocation() }
    }

    @Test
    fun intakeLocation_d04_repoReturnsUnknownError_emitsBus_andDoesNotSetStateError() = runTest {
        initializeIntakeViewModel(
            programIdentifier = 1,
            initialLocationResult = Result.Error(IntakeError.UNKNOWN_ERROR)
        )

        intakeViewModel.state.test {
            awaitItem()
            val loaded = awaitItem()
            assertThat(loaded.locationError).isNull()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { ErrorMessageBus.emit(IntakeError.UNKNOWN_ERROR, any()) }
        coVerify(exactly = 1) { locationRepository.getCurrentLocation() }
    }

    @Test
    fun intakeLocation_d05_retryAfterInitialFailure_succeeds_andSetsLatLng() = runTest {
        coEvery { locationRepository.getCurrentLocation() } returns Result.Error(IntakeError.UNKNOWN_ERROR)

        initializeIntakeViewModel(programIdentifier = 1)

        intakeViewModel.state.test {
            awaitItem()
            awaitItem()

            coVerify(exactly = 1) { ErrorMessageBus.emit(IntakeError.UNKNOWN_ERROR, any()) }

            val location: Location = mockk(relaxed = true) {
                every { latitude } returns -0.1234
                every { longitude } returns 31.5678
            }
            coEvery { locationRepository.getCurrentLocation() } returns Result.Success(location)

            intakeViewModel.onAction(IntakeAction.RetryLocation)

            var afterRetry: IntakeState
            do {
                afterRetry = awaitItem()
            } while (afterRetry.session.latitude == null || afterRetry.session.longitude == null)

            assertThat(afterRetry.locationError).isNull()
            assertThat(afterRetry.session.latitude).isEqualTo(-0.1234f)
            assertThat(afterRetry.session.longitude).isEqualTo(31.5678f)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 2) { locationRepository.getCurrentLocation() }
    }
}
