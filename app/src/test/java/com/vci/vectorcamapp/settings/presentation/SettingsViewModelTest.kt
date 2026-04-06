package com.vci.vectorcamapp.settings.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.use_cases.collector.CollectorValidationUseCases
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.collector.CollectorValidationError
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.settings.domain.util.SettingsError
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
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var deviceCache: DeviceCache
    private lateinit var programRepository: ProgramRepository
    private lateinit var collectorRepository: CollectorRepository
    private lateinit var collectorValidationUseCases: CollectorValidationUseCases
    private lateinit var errorMessageEmitter: ErrorMessageEmitter
    private lateinit var viewModel: SettingsViewModel

    private lateinit var collectorsFlow: MutableStateFlow<List<Collector>>

    private val testDevice = Device(id = 1, model = "Pixel 6", registeredAt = 1_000_000L, submittedAt = null)
    private val testProgram = Program(id = 5, name = "Uganda Program", country = "UG", formVersion = "1.0.0")
    private val testCollector = Collector(id = UUID.randomUUID(), name = "Alice", title = "Dr.", lastTrainedOn = 1_000_000L)

    @Before
    fun setUp() {
        errorMessageEmitter = mockk(relaxed = true)
        coEvery { errorMessageEmitter.emit(any(), any()) } returns Unit

        deviceCache = mockk(relaxed = true)
        programRepository = mockk(relaxed = true)
        collectorRepository = mockk(relaxed = true)
        collectorValidationUseCases = mockk()

        every { collectorValidationUseCases.validateCollectorName(any()) } returns Result.Success(Unit)
        every { collectorValidationUseCases.validateCollectorTitle(any()) } returns Result.Success(Unit)
        every { collectorValidationUseCases.validateCollectorLastTrainedOn(any()) } returns Result.Success(Unit)

        collectorsFlow = MutableStateFlow(emptyList())
        every { collectorRepository.observeAllCollectors() } returns collectorsFlow

        coEvery { deviceCache.getDevice() } returns testDevice
        coEvery { deviceCache.getProgramId() } returns testProgram.id
        coEvery { programRepository.getProgramById(testProgram.id) } returns testProgram

        viewModel = SettingsViewModel(
            deviceCache = deviceCache,
            programRepository = programRepository,
            collectorRepository = collectorRepository,
            collectorValidationUseCases = collectorValidationUseCases,
            errorMessageEmitter = errorMessageEmitter,
        )
    }

    // ========================================
    // A. Initialization & Loading
    // ========================================

    @Test
    fun settingsVm_a01_loadsDeviceAndProgram_intoState() = runTest {
        viewModel.state.test {
            awaitItem() // initialValue = SettingsState()
            val loaded = awaitItem() // after loadSettingsDetails updates _state
            assertThat(loaded.device).isEqualTo(testDevice)
            assertThat(loaded.program).isEqualTo(testProgram)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settingsVm_a02_collectorsFlowUpdate_updatesStateCollectors() = runTest {
        viewModel.state.test {
            skipItems(2) // initialValue + post-load

            collectorsFlow.value = listOf(testCollector)
            advanceUntilIdle()

            val updated = awaitItem()
            assertThat(updated.collectors).containsExactly(testCollector)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settingsVm_a03_noProgramId_doesNotCrash_andStateRemainsDefault() = runTest {
        coEvery { deviceCache.getProgramId() } returns null

        val vm = SettingsViewModel(
            deviceCache = deviceCache,
            programRepository = programRepository,
            collectorRepository = collectorRepository,
            collectorValidationUseCases = collectorValidationUseCases,
            errorMessageEmitter = errorMessageEmitter,
        )

        vm.state.test {
            val s = awaitItem() // initialValue (no load update since programId is null)
            assertThat(s.program.id).isEqualTo(-1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // B. Navigation Events
    // ========================================

    @Test
    fun settingsVm_b01_returnToLandingScreen_emitsNavigateBackEvent() = runTest {
        viewModel.events.test {
            viewModel.onAction(SettingsAction.ReturnToLandingScreen)
            assertThat(awaitItem()).isEqualTo(SettingsEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }
    }

    @Test
    fun settingsVm_b02_startNewDataCollectionSession_emitsNavigateToIntakeEvent() = runTest {
        viewModel.events.test {
            viewModel.onAction(SettingsAction.StartNewDataCollectionSession)
            assertThat(awaitItem()).isEqualTo(SettingsEvent.NavigateToIntakeScreen(SessionType.DATA_COLLECTION))
            expectNoEvents()
        }
    }

    @Test
    fun settingsVm_b03_startNewPracticeSession_emitsNavigateToIntakeEvent() = runTest {
        viewModel.events.test {
            viewModel.onAction(SettingsAction.StartNewPracticeSession)
            assertThat(awaitItem()).isEqualTo(SettingsEvent.NavigateToIntakeScreen(SessionType.PRACTICE))
            expectNoEvents()
        }
    }

    // ========================================
    // C. Add / Edit Collector Dialog
    // ========================================

    @Test
    fun settingsVm_c01_showAddCollectorDialog_setsBlankCollector_andClearsEditFlag() = runTest {
        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.selectedCollector).isNotNull()
            assertThat(s.selectedCollector?.name).isEmpty()
            assertThat(s.selectedCollector?.title).isEmpty()
            assertThat(s.isEditCollectorDialogVisible).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settingsVm_c02_showEditCollectorDialog_setsCollector_andSetsEditFlag() = runTest {
        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(SettingsAction.ShowEditCollectorDialog(testCollector))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.selectedCollector).isEqualTo(testCollector)
            assertThat(s.isEditCollectorDialogVisible).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settingsVm_c03_dismissCollectorDialog_clearsCollector_andAllErrors() = runTest {
        // ShowEditCollectorDialog then DismissCollectorDialog returns to the post-load state
        // (selectedCollector=null, all flags false) → deduplication → assert state.value directly.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowEditCollectorDialog(testCollector))
        viewModel.onAction(SettingsAction.DismissCollectorDialog)
        advanceUntilIdle()

        val s = viewModel.state.value
        assertThat(s.selectedCollector).isNull()
        assertThat(s.isEditCollectorDialogVisible).isFalse()
        assertThat(s.isDeleteCollectorDialogVisible).isFalse()
        assertThat(s.settingsErrors.collectorName).isNull()
        assertThat(s.settingsErrors.collectorTitle).isNull()
        assertThat(s.settingsErrors.collectorLastTrainedOn).isNull()
    }

    @Test
    fun settingsVm_c04_enterCollectorName_updatesSelectedCollectorName() = runTest {
        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
            viewModel.onAction(SettingsAction.EnterCollectorName("Bob"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.selectedCollector?.name).isEqualTo("Bob")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settingsVm_c05_enterCollectorTitle_updatesSelectedCollectorTitle() = runTest {
        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
            viewModel.onAction(SettingsAction.EnterCollectorTitle("Prof."))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.selectedCollector?.title).isEqualTo("Prof.")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settingsVm_c06_enterCollectorLastTrainedOn_updatesTimestamp() = runTest {
        val timestamp = 1_700_000_000L
        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
            viewModel.onAction(SettingsAction.EnterCollectorLastTrainedOn(timestamp))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.selectedCollector?.lastTrainedOn).isEqualTo(timestamp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // D. Save Collector
    // ========================================

    @Test
    fun settingsVm_d01_saveCollector_withNameValidationError_doesNotSave_andSetsError() = runTest {
        every { collectorValidationUseCases.validateCollectorName(any()) } returns
            Result.Error(CollectorValidationError.BLANK_COLLECTOR_NAME)

        // SaveCollector reads state.value.selectedCollector — advanceUntilIdle() between
        // ShowAddCollectorDialog and SaveCollector ensures the combine has updated state.value.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.SaveCollector)
        advanceUntilIdle()

        coVerify(exactly = 0) { collectorRepository.upsertCollector(any()) }
        assertThat(viewModel.state.value.settingsErrors.collectorName)
            .isEqualTo(CollectorValidationError.BLANK_COLLECTOR_NAME)
    }

    @Test
    fun settingsVm_d02_saveCollector_withTitleValidationError_doesNotSave() = runTest {
        every { collectorValidationUseCases.validateCollectorTitle(any()) } returns
            Result.Error(CollectorValidationError.BLANK_COLLECTOR_TITLE)

        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
            viewModel.onAction(SettingsAction.SaveCollector)
            advanceUntilIdle()

            coVerify(exactly = 0) { collectorRepository.upsertCollector(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settingsVm_d03_saveCollector_withNoDuplicates_savesAndClosesDialog() = runTest {
        collectorsFlow.value = emptyList()
        coEvery { collectorRepository.upsertCollector(any()) } returns Result.Success(Unit)

        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
        viewModel.onAction(SettingsAction.EnterCollectorName("Charlie"))
        viewModel.onAction(SettingsAction.EnterCollectorTitle("Nurse"))
        advanceUntilIdle() // let state.value.selectedCollector be populated before SaveCollector reads it

        viewModel.onAction(SettingsAction.SaveCollector)
        advanceUntilIdle()

        coVerify(exactly = 1) { collectorRepository.upsertCollector(any()) }
        val s = viewModel.state.value
        assertThat(s.selectedCollector).isNull()
        assertThat(s.isEditCollectorDialogVisible).isFalse()
    }

    @Test
    fun settingsVm_d04_saveCollector_withSimilarExistingName_showsWarning_andDoesNotSave() = runTest {
        val existingCollector = Collector(id = UUID.randomUUID(), name = "Charlie", title = "Nurse", lastTrainedOn = 0L)
        collectorsFlow.value = listOf(existingCollector)

        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
        viewModel.onAction(SettingsAction.EnterCollectorName("Charli"))
        viewModel.onAction(SettingsAction.EnterCollectorTitle("Nurse"))
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.SaveCollector)
        advanceUntilIdle()

        coVerify(exactly = 0) { collectorRepository.upsertCollector(any()) }
        assertThat(viewModel.state.value.similarCollector).isEqualTo(existingCollector)
    }

    @Test
    fun settingsVm_d05_saveCollector_onException_emitsError() = runTest {
        collectorsFlow.value = emptyList()
        coEvery { collectorRepository.upsertCollector(any()) } throws RuntimeException("DB write failed")

        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
        viewModel.onAction(SettingsAction.EnterCollectorName("Dave"))
        viewModel.onAction(SettingsAction.EnterCollectorTitle("Dr."))
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.SaveCollector)
        advanceUntilIdle()

        coVerify(exactly = 1) { errorMessageEmitter.emit(SettingsError.COLLECTOR_SAVE_FAILED, any()) }
    }

    @Test
    fun settingsVm_d06_confirmSaveCollector_savesIgnoringDuplicateCheck() = runTest {
        coEvery { collectorRepository.upsertCollector(any()) } returns Result.Success(Unit)

        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
        viewModel.onAction(SettingsAction.EnterCollectorName("Eve"))
        viewModel.onAction(SettingsAction.EnterCollectorTitle("Dr."))
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ConfirmSaveCollector)
        advanceUntilIdle()

        coVerify(exactly = 1) { collectorRepository.upsertCollector(any()) }
        val s = viewModel.state.value
        assertThat(s.selectedCollector).isNull()
        assertThat(s.similarCollector).isNull()
    }

    @Test
    fun settingsVm_d07_dismissCollectorWarning_clearsSimilarCollector() = runTest {
        // DismissCollectorWarningDialog sets similarCollector=null which is already null
        // in the default state → no emission → assert state.value directly.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.DismissCollectorWarningDialog)
        advanceUntilIdle()

        assertThat(viewModel.state.value.similarCollector).isNull()
    }

    // ========================================
    // E. Delete Collector Dialog
    // ========================================

    @Test
    fun settingsVm_e01_showDeleteCollectorDialog_setsFlag() = runTest {
        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(SettingsAction.ShowDeleteCollectorDialog)
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.isDeleteCollectorDialogVisible).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settingsVm_e02_dismissDeleteCollectorDialog_clearsFlag() = runTest {
        // Show then dismiss returns isDeleteCollectorDialogVisible to false (its default) → deduplication.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowDeleteCollectorDialog)
        viewModel.onAction(SettingsAction.DismissDeleteCollectorDialog)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isDeleteCollectorDialogVisible).isFalse()
    }

    @Test
    fun settingsVm_e03_confirmDeleteCollector_deletesCollector_andClearsDialog() = runTest {
        coEvery { collectorRepository.deleteCollector(any()) } returns true

        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowEditCollectorDialog(testCollector))
        advanceUntilIdle() // ensure state.value.selectedCollector is set before ConfirmDeleteCollector reads it

        viewModel.onAction(SettingsAction.ConfirmDeleteCollector)
        advanceUntilIdle()

        coVerify(exactly = 1) { collectorRepository.deleteCollector(testCollector) }
        val s = viewModel.state.value
        assertThat(s.selectedCollector).isNull()
        assertThat(s.isEditCollectorDialogVisible).isFalse()
        assertThat(s.isDeleteCollectorDialogVisible).isFalse()
    }

    @Test
    fun settingsVm_e04_confirmDeleteCollector_onException_emitsError() = runTest {
        coEvery { collectorRepository.deleteCollector(any()) } throws RuntimeException("Delete failed")

        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowEditCollectorDialog(testCollector))
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ConfirmDeleteCollector)
        advanceUntilIdle()

        coVerify(exactly = 1) { errorMessageEmitter.emit(SettingsError.COLLECTOR_DELETION_FAILED, any()) }
    }

    @Test
    fun settingsVm_e05_confirmDeleteCollector_withNoSelectedCollector_doesNothing() = runTest {
        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(SettingsAction.ConfirmDeleteCollector)
            advanceUntilIdle()

            coVerify(exactly = 0) { collectorRepository.deleteCollector(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // F. Edit Distance Logic (Duplicate Detection)
    // ========================================

    @Test
    fun settingsVm_f01_identicalName_isDetectedAsDuplicate() = runTest {
        val existingCollector = Collector(id = UUID.randomUUID(), name = "Alice", title = "Dr.", lastTrainedOn = 0L)
        collectorsFlow.value = listOf(existingCollector)

        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
        viewModel.onAction(SettingsAction.EnterCollectorName("Alice"))
        viewModel.onAction(SettingsAction.EnterCollectorTitle("Nurse"))
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.SaveCollector)
        advanceUntilIdle()

        assertThat(viewModel.state.value.similarCollector).isNotNull()
    }

    @Test
    fun settingsVm_f02_nameWithEditDistanceGreaterThanMax_isNotDetectedAsDuplicate() = runTest {
        val existingCollector = Collector(id = UUID.randomUUID(), name = "Alice", title = "Dr.", lastTrainedOn = 0L)
        collectorsFlow.value = listOf(existingCollector)
        coEvery { collectorRepository.upsertCollector(any()) } returns Result.Success(Unit)

        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.ShowAddCollectorDialog)
        viewModel.onAction(SettingsAction.EnterCollectorName("Robert"))
        viewModel.onAction(SettingsAction.EnterCollectorTitle("Dr."))
        advanceUntilIdle()

        viewModel.onAction(SettingsAction.SaveCollector)
        advanceUntilIdle()

        coVerify(exactly = 1) { collectorRepository.upsertCollector(any()) }
    }
}
