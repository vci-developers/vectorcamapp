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
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.repository.SurveillanceFormRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import android.location.Location
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.LocationType
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import com.vci.vectorcamapp.core.data.dto.cache.DefaultIntakeFieldsCacheDto
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteExpression
import com.vci.vectorcamapp.intake.domain.model.FormQuestionPrerequisiteValue
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.domain.repository.LocationRepository
import com.vci.vectorcamapp.intake.domain.strategy.collection_method.CollectionMethodWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.collection_method.CollectionMethodWorkflowFactory
import com.vci.vectorcamapp.intake.domain.strategy.program_form.ProgramFormWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.program_form.ProgramFormWorkflowFactory
import com.vci.vectorcamapp.navigation.Destination
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
    private lateinit var sessionUnitRepository: SessionUnitRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var collectorRepository: CollectorRepository
    private lateinit var programRepository: ProgramRepository
    private lateinit var formAnswerRepository: FormAnswerRepository
    private lateinit var intakeValidationUseCases: IntakeValidationUseCases
    private lateinit var errorMessageEmitter: ErrorMessageEmitter
    private lateinit var transactionHelper: TransactionHelper
    private lateinit var programFormWorkflowFactory: ProgramFormWorkflowFactory
    private lateinit var programFormWorkflow: ProgramFormWorkflow
    private lateinit var collectionMethodWorkflowFactory: CollectionMethodWorkflowFactory
    private lateinit var collectionMethodWorkflow: CollectionMethodWorkflow

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
        isActive = true,
        name = null,
        locationHierarchy = null
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
        sessionUnitRepository = mockk(relaxed = true)
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

        collectionMethodWorkflow = mockk()
        every { collectionMethodWorkflow.postIntakeDestination } returns Destination.Imaging(null)
        collectionMethodWorkflowFactory = mockk()
        every { collectionMethodWorkflowFactory.create(any(), any()) } returns collectionMethodWorkflow

        collectorsFlow = MutableStateFlow(emptyList())
        every { collectorRepository.observeAllCollectors() } returns collectorsFlow

        coEvery { deviceCache.getProgramId() } returns testProgramId
        coEvery { programRepository.getProgramById(testProgramId) } returns testProgram
        coEvery { currentSessionCache.getSession() } returns null
        coEvery { defaultIntakeFieldsCache.getDefaultIntakeFields() } returns null
        coEvery { formAnswerRepository.getSessionScopedFormAnswers(any()) } returns emptyMap()

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
            sessionUnitRepository = sessionUnitRepository,
            locationRepository = locationRepository,
            collectorRepository = collectorRepository,
            programRepository = programRepository,
            formAnswerRepository = formAnswerRepository,
            errorMessageEmitter = errorMessageEmitter,
        ).also { vm ->
            vm.transactionHelper = transactionHelper
            vm.programFormWorkflowFactory = programFormWorkflowFactory
            vm.collectionMethodWorkflowFactory = collectionMethodWorkflowFactory
        }
    }

    private fun runTransactions() {
        coEvery { transactionHelper.runAsTransaction<Boolean>(any()) } coAnswers {
            firstArg<suspend () -> Boolean>().invoke()
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

    @Test
    fun intakeVm_g02_locationSuccessTimeoutAndFailures_updateSessionOrError() = runTest {
        val location = mockk<Location>()
        every { location.latitude } returns 1.25
        every { location.longitude } returns 32.5
        coEvery { locationRepository.getCurrentLocation() } returns Result.Success(location)
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        assertThat(vm.state.value.session.latitude).isEqualTo(1.25f)
        assertThat(vm.state.value.session.longitude).isEqualTo(32.5f)

        coEvery { locationRepository.getCurrentLocation() } throws SecurityException("denied")
        vm.onAction(IntakeAction.RetryLocation)
        advanceUntilIdle()
        coVerify { errorMessageEmitter.emit(IntakeError.LOCATION_PERMISSION_DENIED, any()) }

        coEvery { locationRepository.getCurrentLocation() } throws IllegalStateException("gps")
        vm.onAction(IntakeAction.RetryLocation)
        advanceUntilIdle()
        coVerify { errorMessageEmitter.emit(IntakeError.UNKNOWN_ERROR, any()) }

        coEvery { locationRepository.getCurrentLocation() } coAnswers {
            kotlinx.coroutines.delay(31_000)
            Result.Success(location)
        }
        vm.onAction(IntakeAction.RetryLocation)
        advanceUntilIdle()
        assertThat(vm.state.value.locationError).isEqualTo(IntakeError.LOCATION_GPS_TIMEOUT)
    }

    @Test
    fun intakeVm_h01_surveillanceAndLocationInputs_updateForm() = runTest {
        every { programFormWorkflow.surveillanceForm } returns surveillanceForm()
        val types = listOf(LocationType(1, "Village", 1), LocationType(2, "House", 2))
        every { locationTypeRepository.observeAllLocationTypesByProgramId(any()) } returns MutableStateFlow(types)
        val requiresYes = FormQuestionPrerequisiteExpression.Predicate(
            1, "eq", FormQuestionPrerequisiteValue.StringValue("yes")
        )
        every { programFormWorkflow.formQuestions } returns listOf(
            question(1, "text"),
            question(2, "boolean", requiresYes),
            question(3, "text", requiresYes),
        )
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(IntakeAction.EnterNumPeopleSleptInHouse("02"))
        vm.onAction(IntakeAction.ToggleIrsConducted(true))
        vm.onAction(IntakeAction.EnterMonthsSinceIrs("4"))
        vm.onAction(IntakeAction.EnterNumLlinsAvailable("0"))
        vm.onAction(IntakeAction.EnterNumLlinsAvailable("2"))
        vm.onAction(IntakeAction.SelectLlinType(IntakeDropdownOptions.LlinTypeOption.PYRETHROID_ONLY))
        vm.onAction(IntakeAction.SelectLlinBrand(IntakeDropdownOptions.LlinBrandOption.OLYSET_NET))
        vm.onAction(IntakeAction.EnterNumPeopleSleptUnderLlin("1"))
        vm.onAction(IntakeAction.SelectLocationTypeSiteOption(1, "Kampala"))
        vm.onAction(IntakeAction.SelectLocationTypeSiteOption(2, "12"))
        vm.onAction(IntakeAction.SelectLocationTypeSiteOption(1, "Jinja"))
        vm.onAction(IntakeAction.SelectLocationTypeSiteOption(99, "ignored"))
        vm.onAction(IntakeAction.UpdateFormAnswer(2, "true"))
        vm.onAction(IntakeAction.UpdateFormAnswer(3, "note"))
        vm.onAction(IntakeAction.UpdateFormAnswer(1, "no"))
        vm.onAction(IntakeAction.UpdateFormAnswer(44, "missing"))
        vm.onAction(IntakeAction.EnterNumPeopleSleptInHouse("abc"))
        advanceUntilIdle()

        val form = vm.state.value.surveillanceForm!!
        assertThat(form.wasIrsConducted).isTrue()
        assertThat(form.numLlinsAvailable).isEqualTo(2)
        assertThat(form.llinBrand).isEqualTo("OLYSET Net")
        assertThat(vm.state.value.siteSelectionsByLocationTypeId).containsEntry(1, "Jinja")
        assertThat(vm.state.value.siteSelectionsByLocationTypeId).doesNotContainKey(2)
        assertThat(vm.state.value.formAnswersByQuestionId.getValue(2).value).isEqualTo("false")
        assertThat(vm.state.value.formAnswersByQuestionId.getValue(3).value).isEqualTo("")
    }

    @Test
    fun intakeVm_h02_registerMissingCollector_savesOrReportsFailure() = runTest {
        coEvery { defaultIntakeFieldsCache.getDefaultIntakeFields() } returns DefaultIntakeFieldsCacheDto(
            collectorName = "Bob",
            collectorTitle = "VCO",
        )
        coEvery { collectorRepository.upsertCollector(any()) } returns Result.Success(Unit)
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(IntakeAction.RegisterMissingCollector)
        advanceUntilIdle()
        assertThat(vm.state.value.isCurrentCollectorMissing).isFalse()

        coEvery { collectorRepository.upsertCollector(any()) } returns Result.Error(RoomDbError.UNKNOWN_ERROR)
        vm.onAction(IntakeAction.EnterHardwareId("hw"))
        // Name is already set; force the error path by calling register again after flipping the flag via a blank-then-restore.
        vm.onAction(IntakeAction.RegisterMissingCollector)
        advanceUntilIdle()
        coVerify { errorMessageEmitter.emit(IntakeError.COLLECTOR_SAVE_FAILED, any()) }
    }

    @Test
    fun intakeVm_h03_blankCollector_doesNotRegister() = runTest {
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(IntakeAction.RegisterMissingCollector)
        advanceUntilIdle()
        coVerify(exactly = 0) { collectorRepository.upsertCollector(any()) }
    }

    @Test
    fun intakeVm_h04_lockedCollectionMethod_isIgnored() = runTest {
        val session = sampleSession()
        coEvery { currentSessionCache.getSession() } returns session
        coEvery { sessionUnitRepository.countSessionUnitsForSession(session.localId) } returns 1
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(IntakeAction.UpdateCollectionMethod("Net"))
        advanceUntilIdle()
        assertThat(vm.state.value.session.collectionMethod).isEqualTo(session.collectionMethod)
        assertThat(vm.state.value.isCollectionMethodLocked).isTrue()
    }

    @Test
    fun intakeVm_i01_submit_invalidForm_emitsFormInvalid() = runTest {
        every { programFormWorkflow.surveillanceForm } returns surveillanceForm().copy(
            wasIrsConducted = true,
            monthsSinceIrs = -1,
            llinType = "",
            llinBrand = "",
            numPeopleSleptUnderLlin = -1,
            numPeopleSleptInHouse = -1,
            numLlinsAvailable = -1,
        )
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        vm.onAction(IntakeAction.SubmitIntakeForm)
        advanceUntilIdle()

        coVerify { errorMessageEmitter.emit(IntakeError.FORM_INVALID, any()) }
        coVerify(exactly = 0) { transactionHelper.runAsTransaction<Boolean>(any()) }
    }

    @Test
    fun intakeVm_i02_submit_missingCollector_emitsMissingCollector() = runTest {
        coEvery { defaultIntakeFieldsCache.getDefaultIntakeFields() } returns DefaultIntakeFieldsCacheDto(
            collectorName = "Bob",
            collectorTitle = "VCO",
            district = "District A",
            villageName = "Village A",
        )
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()
        vm.onAction(IntakeAction.SelectHouseNumber("101"))
        vm.onAction(IntakeAction.UpdateCollectionMethod("CDC Light Trap (LTC)"))
        vm.onAction(IntakeAction.UpdateSpecimenCondition("Fresh"))
        advanceUntilIdle()

        vm.onAction(IntakeAction.SubmitIntakeForm)
        advanceUntilIdle()

        coVerify { errorMessageEmitter.emit(IntakeError.MISSING_COLLECTOR, any()) }
    }

    @Test
    fun intakeVm_i03_submit_siteNotFound() = runTest {
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()
        fillCollectorAndMethod(vm)
        vm.onAction(IntakeAction.SelectDistrict("Missing"))
        vm.onAction(IntakeAction.SelectVillageName("Nowhere"))
        vm.onAction(IntakeAction.SelectHouseNumber("0"))
        advanceUntilIdle()

        vm.onAction(IntakeAction.SubmitIntakeForm)
        advanceUntilIdle()

        coVerify { errorMessageEmitter.emit(IntakeError.SITE_NOT_FOUND, any()) }
    }

    @Test
    fun intakeVm_i04_submit_districtSite_navigates() = runTest {
        runTransactions()
        coEvery { sessionRepository.upsertSession(any(), any()) } returns Result.Success(Unit)
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()
        fillCollectorAndMethod(vm)
        vm.onAction(IntakeAction.SelectDistrict("District A"))
        vm.onAction(IntakeAction.SelectVillageName("Village A"))
        vm.onAction(IntakeAction.SelectHouseNumber("101"))
        advanceUntilIdle()

        vm.events.test {
            vm.onAction(IntakeAction.SubmitIntakeForm)
            assertThat(awaitItem()).isEqualTo(IntakeEvent.NavigateAfterIntake(Destination.Imaging(null)))
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { currentSessionCache.saveSession(any(), testSite.id) }
        coVerify { defaultIntakeFieldsCache.saveDefaultIntakeFields(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun intakeVm_i05_submit_hierarchySite_persistsSurveillanceFormAndAnswers() = runTest {
        val hierarchySite = testSite.copy(
            district = null,
            villageName = null,
            houseNumber = null,
            locationHierarchy = mapOf("Village" to "Kampala", "House" to "12"),
        )
        every { siteRepository.observeAllSitesByProgramId(any()) } returns MutableStateFlow(listOf(hierarchySite))
        every { locationTypeRepository.observeAllLocationTypesByProgramId(any()) } returns MutableStateFlow(
            listOf(LocationType(1, "Village", 1), LocationType(2, "House", 2))
        )
        every { programFormWorkflow.surveillanceForm } returns surveillanceForm()
        every { programFormWorkflow.formQuestions } returns listOf(question(1, "text"))
        runTransactions()
        coEvery { sessionRepository.upsertSession(any(), any()) } returns Result.Success(Unit)
        coEvery { surveillanceFormRepository.upsertSurveillanceForm(any(), any()) } returns Result.Success(Unit)
        coEvery { formAnswerRepository.upsertFormAnswer(any(), any(), any(), any()) } returns Result.Success(Unit)

        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()
        fillCollectorAndMethod(vm)
        vm.onAction(IntakeAction.SelectLocationTypeSiteOption(1, "Kampala"))
        vm.onAction(IntakeAction.SelectLocationTypeSiteOption(2, "12"))
        advanceUntilIdle()

        vm.events.test {
            vm.onAction(IntakeAction.SubmitIntakeForm)
            assertThat(awaitItem()).isEqualTo(IntakeEvent.NavigateAfterIntake(Destination.Imaging(null)))
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { surveillanceFormRepository.upsertSurveillanceForm(any(), any()) }
        coVerify { formAnswerRepository.upsertFormAnswer(any(), any(), isNull(), 1) }
        coVerify {
            defaultIntakeFieldsCache.saveDefaultIntakeFields(
                any(), any(), any(), any(), any(), any(),
                match { it.keys == setOf(1) },
            )
        }
    }

    @Test
    fun intakeVm_i06_submit_repositoryFailures_doNotNavigate() = runTest {
        every { programFormWorkflow.surveillanceForm } returns surveillanceForm()
        every { programFormWorkflow.formQuestions } returns listOf(question(1, "text"))
        runTransactions()
        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()
        fillCollectorAndMethod(vm)
        vm.onAction(IntakeAction.SelectDistrict("District A"))
        vm.onAction(IntakeAction.SelectVillageName("Village A"))
        vm.onAction(IntakeAction.SelectHouseNumber("101"))
        advanceUntilIdle()

        coEvery { sessionRepository.upsertSession(any(), any()) } returns Result.Error(RoomDbError.UNKNOWN_ERROR)
        vm.events.test {
            vm.onAction(IntakeAction.SubmitIntakeForm)
            advanceUntilIdle()
            expectNoEvents()
        }

        coEvery { sessionRepository.upsertSession(any(), any()) } returns Result.Success(Unit)
        coEvery { surveillanceFormRepository.upsertSurveillanceForm(any(), any()) } returns
            Result.Error(RoomDbError.CONSTRAINT_VIOLATION)
        vm.onAction(IntakeAction.SubmitIntakeForm)
        advanceUntilIdle()

        coEvery { surveillanceFormRepository.upsertSurveillanceForm(any(), any()) } returns Result.Success(Unit)
        coEvery { formAnswerRepository.upsertFormAnswer(any(), any(), any(), any()) } returns
            Result.Error(RoomDbError.NO_ROWS_AFFECTED)
        vm.onAction(IntakeAction.SubmitIntakeForm)
        advanceUntilIdle()

        coVerify { errorMessageEmitter.emit(RoomDbError.UNKNOWN_ERROR, any()) }
        coVerify { errorMessageEmitter.emit(RoomDbError.CONSTRAINT_VIOLATION, any()) }
        coVerify { errorMessageEmitter.emit(RoomDbError.NO_ROWS_AFFECTED, any()) }
    }

    @Test
    fun intakeVm_i07_load_restoresHierarchySelectionsAndCachedDistrict() = runTest {
        val session = sampleSession()
        val hierarchySite = testSite.copy(
            locationHierarchy = mapOf("Village" to "Kampala"),
        )
        coEvery { currentSessionCache.getSession() } returns session
        coEvery { currentSessionCache.getSiteId() } returns hierarchySite.id
        coEvery { defaultIntakeFieldsCache.getDefaultIntakeFields() } returns DefaultIntakeFieldsCacheDto(
            district = "District A",
            villageName = "Village A",
            locationSelections = mapOf(9 to "cached"),
        )
        every { siteRepository.observeAllSitesByProgramId(any()) } returns MutableStateFlow(listOf(hierarchySite))
        every { locationTypeRepository.observeAllLocationTypesByProgramId(any()) } returns MutableStateFlow(
            listOf(LocationType(1, "Village", 1))
        )
        every { surveillanceFormRepository.observeSurveillanceFormBySessionId(session.localId) } returns
            flowOf(surveillanceForm())

        val vm = makeViewModel()
        backgroundScope.launch { vm.state.collect {} }
        advanceUntilIdle()

        assertThat(vm.state.value.siteSelectionsByLocationTypeId).containsEntry(1, "Kampala")
        assertThat(vm.state.value.selectedDistrict).isEqualTo("District A")
        assertThat(vm.state.value.surveillanceForm).isEqualTo(surveillanceForm())
    }

    private fun fillCollectorAndMethod(vm: IntakeViewModel) {
        collectorsFlow.value = listOf(testCollector)
        vm.onAction(IntakeAction.SelectCollector(testCollector))
        vm.onAction(IntakeAction.UpdateCollectionMethod("CDC Light Trap (LTC)"))
        vm.onAction(IntakeAction.UpdateSpecimenCondition("Fresh"))
    }

    private fun surveillanceForm() = SurveillanceForm(
        numPeopleSleptInHouse = 2,
        wasIrsConducted = false,
        monthsSinceIrs = null,
        numLlinsAvailable = 1,
        llinType = "Pyrethroid Only",
        llinBrand = "OLYSET Net",
        numPeopleSleptUnderLlin = 1,
        submittedAt = null,
    )

    private fun question(
        id: Int,
        type: String,
        prerequisite: FormQuestionPrerequisiteExpression? = null,
    ) = FormQuestion(
        id = id,
        label = "q$id",
        type = type,
        required = false,
        prerequisite = prerequisite,
        options = null,
        order = id,
        answerScope = FormQuestionScope.SESSION,
        isUnitIdentityComponent = false,
    )

    private fun sampleSession() = Session(
        localId = UUID.fromString("00000000-0000-0000-0000-000000000071"),
        remoteId = null,
        hardwareId = "HW",
        collectorTitle = "Dr.",
        collectorName = "Alice",
        collectorLastTrainedOn = 0L,
        collectionDate = 1_000L,
        collectionMethod = "Net",
        specimenCondition = "Fresh",
        createdAt = 1_000L,
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = 1f,
        longitude = 2f,
        type = SessionType.SURVEILLANCE,
    )
}
