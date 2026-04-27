package com.vci.vectorcamapp.intake.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DefaultIntakeFieldsCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.LocationTypeRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.repository.SurveillanceFormRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.intake.domain.repository.LocationRepository
import com.vci.vectorcamapp.intake.domain.strategy.ProgramFormWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.ProgramFormWorkflowFactory
import com.vci.vectorcamapp.intake.domain.use_cases.IntakeValidationUseCases
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateCollectionDateUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateCollectionMethodUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateCollectorUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateDistrictUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateFormAnswersUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateHouseNumberUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateLlinBrandUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateLlinTypeUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateMonthsSinceIrsUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateNumLlinsAvailableUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateNumPeopleSleptInHouseUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateNumPeopleSleptUnderLlinUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateSpecimenConditionUseCase
import com.vci.vectorcamapp.intake.domain.use_cases.ValidateVillageNameUseCase
import com.vci.vectorcamapp.intake.domain.util.IntakeError
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
class IntakeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var deviceCache: DeviceCache
    private lateinit var currentSessionCache: CurrentSessionCache
    private lateinit var defaultIntakeFieldsCache: DefaultIntakeFieldsCache
    private lateinit var siteRepository: SiteRepository
    private lateinit var locationTypeRepository: LocationTypeRepository
    private lateinit var surveillanceFormRepository: SurveillanceFormRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var collectorRepository: CollectorRepository
    private lateinit var programRepository: ProgramRepository
    private lateinit var formAnswerRepository: FormAnswerRepository
    private lateinit var intakeValidationUseCases: IntakeValidationUseCases
    private lateinit var errorMessageEmitter: ErrorMessageEmitter
    private lateinit var transactionHelper: TransactionHelper
    private lateinit var programFormWorkflowFactory: ProgramFormWorkflowFactory
    private lateinit var programFormWorkflow: ProgramFormWorkflow

    private lateinit var collectorsFlow: MutableStateFlow<List<Collector>>

    private val testProgramId = 1
    private val testProgram = Program(id = testProgramId, name = "Test Program", country = "UG", formVersion = "1.0.0")
    private val testCollector = Collector(id = UUID.randomUUID(), name = "Alice", title = "Dr.", lastTrainedOn = 0L)
    private val testSite = Site(
        id = 1,
        district = "District A",
        subCounty = "SubCounty A",
        parish = "Parish A",
        villageName = "Village A",
        houseNumber = "101",
        healthCenter = "HC A",
        isActive = true
    )

    @Before
    fun setUp() {
        errorMessageEmitter = mockk(relaxed = true)
        coEvery { errorMessageEmitter.emit(any(), any()) } returns Unit

        deviceCache = mockk(relaxed = true)
        currentSessionCache = mockk(relaxed = true)
        defaultIntakeFieldsCache = mockk(relaxed = true)
        siteRepository = mockk(relaxed = true)
        locationTypeRepository = mockk(relaxed = true)
        surveillanceFormRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        locationRepository = mockk(relaxed = true)
        collectorRepository = mockk(relaxed = true)
        programRepository = mockk(relaxed = true)
        formAnswerRepository = mockk(relaxed = true)
        transactionHelper = mockk(relaxed = true)

        programFormWorkflow = mockk(relaxed = true)
        every { programFormWorkflow.surveillanceForm } returns null
        every { programFormWorkflow.form } returns null
        every { programFormWorkflow.formQuestions } returns emptyList()

        programFormWorkflowFactory = mockk()
        coEvery { programFormWorkflowFactory.create(any(), any()) } returns programFormWorkflow

        collectorsFlow = MutableStateFlow(emptyList())
        every { collectorRepository.observeAllCollectors() } returns collectorsFlow

        coEvery { deviceCache.getProgramId() } returns testProgramId
        coEvery { programRepository.getProgramById(testProgramId) } returns testProgram
        coEvery { currentSessionCache.getSession() } returns null
        coEvery { defaultIntakeFieldsCache.getDefaultIntakeFields() } returns null
        coEvery { formAnswerRepository.getFormAnswersBySessionId(any()) } returns emptyMap()

        every { siteRepository.observeAllSitesByProgramId(any()) } returns MutableStateFlow(listOf(testSite))
        every { surveillanceFormRepository.observeSurveillanceFormBySessionId(any()) } returns flowOf(null)
        every { locationTypeRepository.observeAllLocationTypesByProgramId(any()) } returns MutableStateFlow(emptyList())

        coEvery { locationRepository.getCurrentLocation() } returns Result.Error(IntakeError.LOCATION_PERMISSION_DENIED)

        intakeValidationUseCases = IntakeValidationUseCases(
            validateCollector = ValidateCollectorUseCase(),
            validateDistrict = ValidateDistrictUseCase(),
            validateVillageName = ValidateVillageNameUseCase(),
            validateHouseNumber = ValidateHouseNumberUseCase(),
            validateLlinType = ValidateLlinTypeUseCase(),
            validateLlinBrand = ValidateLlinBrandUseCase(),
            validateCollectionDate = ValidateCollectionDateUseCase(),
            validateCollectionMethod = ValidateCollectionMethodUseCase(),
            validateSpecimenCondition = ValidateSpecimenConditionUseCase(),
            validateNumPeopleSleptInHouse = ValidateNumPeopleSleptInHouseUseCase(),
            validateMonthsSinceIrs = ValidateMonthsSinceIrsUseCase(),
            validateNumLlinsAvailable = ValidateNumLlinsAvailableUseCase(),
            validateNumPeopleSleptUnderLlin = ValidateNumPeopleSleptUnderLlinUseCase(),
            validateFormAnswersUseCase = ValidateFormAnswersUseCase(),
        )
    }

    private fun makeViewModel(
        sessionType: SessionType = SessionType.SURVEILLANCE
    ): IntakeViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("sessionType" to sessionType))
        return IntakeViewModel(
            savedStateHandle = savedStateHandle,
            intakeValidationUseCases = intakeValidationUseCases,
            deviceCache = deviceCache,
            currentSessionCache = currentSessionCache,
            defaultIntakeFieldsCache = defaultIntakeFieldsCache,
            siteRepository = siteRepository,
            locationTypeRepository = locationTypeRepository,
            surveillanceFormRepository = surveillanceFormRepository,
            sessionRepository = sessionRepository,
            locationRepository = locationRepository,
            collectorRepository = collectorRepository,
            programRepository = programRepository,
            formAnswerRepository = formAnswerRepository,
            errorMessageEmitter = errorMessageEmitter,
        ).also { vm ->
            vm.transactionHelper = transactionHelper
            vm.programFormWorkflowFactory = programFormWorkflowFactory
        }
    }

    // ========================================
    // A. Initialization & Loading
    // ========================================

    @Test
    fun intakeVm_a01_noProgramId_emitsError_andNavigatesBack() = runTest {
        coEvery { deviceCache.getProgramId() } returns null
        val vm = makeViewModel()

        backgroundScope.launch { vm.state.collect {} }

        vm.events.test {
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(IntakeEvent.NavigateBackToRegistrationScreen)
            expectNoEvents()
        }

        coVerify(atLeast = 1) { errorMessageEmitter.emit(IntakeError.PROGRAM_NOT_FOUND, any()) }
    }

    @Test
    fun intakeVm_a02_programNotFound_emitsError_andNavigatesBack() = runTest {
        coEvery { programRepository.getProgramById(testProgramId) } returns null
        val vm = makeViewModel()

        backgroundScope.launch { vm.state.collect {} }

        vm.events.test {
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(IntakeEvent.NavigateBackToRegistrationScreen)
            expectNoEvents()
        }

        coVerify(atLeast = 1) { errorMessageEmitter.emit(IntakeError.PROGRAM_NOT_FOUND, any()) }
    }

    // ========================================
    // B. Navigation
    // ========================================

    @Test
    fun intakeVm_b01_returnToPreviousScreen_clearsSession_andEmitsEvent() = runTest {
        val vm = makeViewModel()

        vm.events.test {
            vm.onAction(IntakeAction.ReturnToPreviousScreen)
            assertThat(awaitItem()).isEqualTo(IntakeEvent.NavigateBackToPreviousScreen)
            expectNoEvents()
        }

        coVerify(exactly = 1) { currentSessionCache.clearSession() }
    }

    // ========================================
    // C. Field State Updates
    // ========================================

    @Test
    fun intakeVm_c01_enterHardwareId_updatesSession() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3) // initialValue, isLoading=true, isLoading=false (loaded)

            vm.onAction(IntakeAction.EnterHardwareId("DEVICE_001"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.session.hardwareId).isEqualTo("DEVICE_001")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun intakeVm_c02_enterNotes_updatesSession() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            vm.onAction(IntakeAction.EnterNotes("Some field notes"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.session.notes).isEqualTo("Some field notes")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun intakeVm_c03_pickCollectionDate_updatesSession() = runTest {
        val vm = makeViewModel()
        val newDate = 1_700_000_000L

        vm.state.test {
            skipItems(3)

            vm.onAction(IntakeAction.PickCollectionDate(newDate))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.session.collectionDate).isEqualTo(newDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun intakeVm_c04_updateCollectionMethod_updatesSession() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            vm.onAction(IntakeAction.UpdateCollectionMethod("Aspirator"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.session.collectionMethod).isEqualTo("Aspirator")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun intakeVm_c05_updateSpecimenCondition_updatesSession() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            vm.onAction(IntakeAction.UpdateSpecimenCondition("Excellent"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.session.specimenCondition).isEqualTo("Excellent")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun intakeVm_c06_selectCollector_updatesSessionCollectorFields_andClearsCollectorMissingFlag() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            vm.onAction(IntakeAction.SelectCollector(testCollector))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.session.collectorName).isEqualTo("Alice")
            assertThat(s.session.collectorTitle).isEqualTo("Dr.")
            assertThat(s.isCurrentCollectorMissing).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // D. District / Village / House Selection
    // ========================================

    @Test
    fun intakeVm_d01_selectDistrict_updatesDistrict_andClearsVillageAndHouseNumber() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            vm.onAction(IntakeAction.SelectVillageName("Village A"))
            vm.onAction(IntakeAction.SelectHouseNumber("123"))
            vm.onAction(IntakeAction.SelectDistrict("District B"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.selectedDistrict).isEqualTo("District B")
            assertThat(s.selectedVillageName).isEmpty()
            assertThat(s.selectedHouseNumber).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun intakeVm_d02_selectVillageName_updatesVillage_andClearsHouseNumber() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            vm.onAction(IntakeAction.SelectHouseNumber("456"))
            vm.onAction(IntakeAction.SelectVillageName("Village B"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.selectedVillageName).isEqualTo("Village B")
            assertThat(s.selectedHouseNumber).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun intakeVm_d03_selectHouseNumber_updatesHouseNumber() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            vm.onAction(IntakeAction.SelectHouseNumber("789"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.selectedHouseNumber).isEqualTo("789")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // E. Tooltip Visibility
    // ========================================

    @Test
    fun intakeVm_e01_showCollectionMethodTooltip_setsVisible() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            vm.onAction(IntakeAction.ShowCollectionMethodTooltipDialog)
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.isCollectionMethodTooltipVisible).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun intakeVm_e02_hideCollectionMethodTooltip_clearsVisible() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            // Show first so the StateFlow value is 'true', then Hide changes it to 'false'.
            // Dispatching both together would cancel each other out (false → false),
            // causing StateFlow to deduplicate and emit nothing.
            vm.onAction(IntakeAction.ShowCollectionMethodTooltipDialog)
            advanceUntilIdle()
            skipItems(1) // consume the intermediate 'true' emission

            vm.onAction(IntakeAction.HideCollectionMethodTooltipDialog)
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.isCollectionMethodTooltipVisible).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // F. Collectors Flow
    // ========================================

    @Test
    fun intakeVm_f01_collectorsFlowUpdate_updatesStateCollectors() = runTest {
        val vm = makeViewModel()

        vm.state.test {
            skipItems(3)

            collectorsFlow.value = listOf(testCollector)
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.allCollectors).containsExactly(testCollector)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // G. Location Retry
    // ========================================

    @Test
    fun intakeVm_g01_retryLocation_clearsLocationError() = runTest {
        // GPS_TIMEOUT causes getLocation() to set locationError in state.
        coEvery { locationRepository.getCurrentLocation() } returns Result.Error(IntakeError.LOCATION_GPS_TIMEOUT)
        val vm = makeViewModel()

        // Trigger onStart and let loadFormDetails run to completion.
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        assertThat(vm.state.value.locationError).isEqualTo(IntakeError.LOCATION_GPS_TIMEOUT)

        // Switch mock to PERMISSION_DENIED so the retry getLocation() calls emitError()
        // instead of setting locationError back — leaving locationError = null after RetryLocation.
        coEvery { locationRepository.getCurrentLocation() } returns Result.Error(IntakeError.LOCATION_PERMISSION_DENIED)

        vm.onAction(IntakeAction.RetryLocation)
        advanceUntilIdle()

        // RetryLocation clears locationError at the start; the subsequent getLocation() with
        // PERMISSION_DENIED calls emitError() (no state update), so locationError stays null.
        assertThat(vm.state.value.locationError).isNull()
    }
}
