package com.vci.vectorcamapp.registration.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.dto.program.GetAllProgramsResponseDto
import com.vci.vectorcamapp.core.data.dto.program.ProgramDto
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.network.api.LocationTypeDataSource
import com.vci.vectorcamapp.core.domain.network.api.ProgramDataSource
import com.vci.vectorcamapp.core.domain.network.api.SiteDataSource
import com.vci.vectorcamapp.core.domain.network.connectivity.ConnectivityObserver
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.repository.LocationTypeRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.use_cases.collector.CollectorValidationUseCases
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.registration.domain.util.RegistrationError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var deviceCache: DeviceCache
    private lateinit var sessionCache: CurrentSessionCache
    private lateinit var programRepository: ProgramRepository
    private lateinit var programDataSource: ProgramDataSource
    private lateinit var siteDataSource: SiteDataSource
    private lateinit var siteRepository: SiteRepository
    private lateinit var locationTypeDataSource: LocationTypeDataSource
    private lateinit var locationTypeRepository: LocationTypeRepository
    private lateinit var transactionHelper: TransactionHelper
    private lateinit var connectivityObserver: ConnectivityObserver

    private lateinit var collectorRepository: CollectorRepository
    private lateinit var collectorValidationUseCases: CollectorValidationUseCases
    private lateinit var errorMessageEmitter: ErrorMessageEmitter

    private lateinit var viewModel: RegistrationViewModel
    private lateinit var programsFlow: MutableStateFlow<List<Program>>

    private val testPrograms = listOf(
        Program(id = 1, name = "Program 1", country = "Country 1"),
        Program(id = 2, name = "Program 2", country = "Country 2")
    )

    @Before
    fun setUp() {
        errorMessageEmitter = mockk(relaxed = true)
        coEvery { errorMessageEmitter.emit(any(), any()) } returns Unit
        every { errorMessageEmitter.clearLastMessage() } returns Unit

        deviceCache = mockk(relaxed = true)
        sessionCache = mockk(relaxed = true)
        programRepository = mockk()
        programDataSource = mockk()
        coEvery { programDataSource.getAllPrograms() } returns Result.Success(
            GetAllProgramsResponseDto(
                programs = testPrograms.map {
                    ProgramDto(programId = it.id, name = it.name, country = it.country)
                }
            )
        )
        siteDataSource = mockk()
        siteRepository = mockk()
        locationTypeDataSource = mockk()
        locationTypeRepository = mockk()
        transactionHelper = mockk(relaxed = true)
        connectivityObserver = mockk()
        every { connectivityObserver.isConnected } returns flowOf(true)

        collectorRepository = mockk(relaxed = true)
        coEvery { collectorRepository.upsertCollector(any()) } returns Result.Success(Unit)

        collectorValidationUseCases = mockk()
        every { collectorValidationUseCases.validateCollectorName(any()) } returns Result.Success(Unit)
        every { collectorValidationUseCases.validateCollectorTitle(any()) } returns Result.Success(Unit)
        every { collectorValidationUseCases.validateCollectorLastTrainedOn(any()) } returns Result.Success(Unit)

        programsFlow = MutableStateFlow(testPrograms)
        every { programRepository.observeAllPrograms() } returns programsFlow

        viewModel = RegistrationViewModel(
            transactionHelper = transactionHelper,
            deviceCache = deviceCache,
            currentSessionCache = sessionCache,
            collectorRepository = collectorRepository,
            collectorValidationUseCases = collectorValidationUseCases,
            programDataSource = programDataSource,
            programRepository = programRepository,
            siteDataSource = siteDataSource,
            siteRepository = siteRepository,
            locationTypeDataSource = locationTypeDataSource,
            locationTypeRepository = locationTypeRepository,
            connectivityObserver = connectivityObserver,
            errorMessageEmitter = errorMessageEmitter
        )
    }

    // ========================================
    // Test Harness / Helpers
    // ========================================

    private fun selectProgram(program: Program) = runTest {
        viewModel.state.test {
            awaitItem()
            awaitItem()
            viewModel.onAction(RegistrationAction.SelectProgram(program))
            advanceUntilIdle()
            val selectedState = awaitItem()
            assertThat(selectedState.selectedProgram).isEqualTo(program)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // A. State Management
    // ========================================

    @Test
    fun regVm_a01_initialStateHasEmptyProgramsAndNoSelection() = runTest {
        advanceUntilIdle()

        viewModel.state.test {
            val initialState = awaitItem()
            assertThat(initialState.programs).isEmpty()
            assertThat(initialState.selectedProgram).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun regVm_a02_stateEmitsProgramsFromRepositoryAfterInitialization() = runTest {
        viewModel.state.test {
            awaitItem()
            advanceUntilIdle()
            awaitItem()
            val stateWithPrograms = awaitItem()
            assertThat(stateWithPrograms.programs).isEqualTo(testPrograms)
            assertThat(stateWithPrograms.selectedProgram).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun regVm_a03_stateUpdatesWhenRepositoryProgramsChange() = runTest {
        advanceUntilIdle()

        viewModel.state.test {
            awaitItem()
            awaitItem()

            val newPrograms = listOf(Program(id = 99, name = "Updated", country = "Test"))
            programsFlow.value = newPrograms
            advanceUntilIdle()

            val updatedState = awaitItem()
            assertThat(updatedState.programs).isEqualTo(newPrograms)
        }
    }

    // ========================================
    // B. Program Selection
    // ========================================

    @Test
    fun regVm_b01_selectingProgramUpdatesStateCorrectly() = runTest {
        val programToSelect = testPrograms[0]
        advanceUntilIdle()

        viewModel.state.test {
            awaitItem()
            awaitItem()

            viewModel.onAction(RegistrationAction.SelectProgram(programToSelect))
            advanceUntilIdle()

            val updatedState = awaitItem()
            assertThat(updatedState.selectedProgram).isEqualTo(programToSelect)
            assertThat(updatedState.programs).isEqualTo(testPrograms)
        }
    }

    @Test
    fun regVm_b02_programSelectionPersistsWhenRepositoryUpdates() = runTest {
        val selectedProgram = testPrograms[0]
        advanceUntilIdle()

        viewModel.state.test {
            awaitItem()
            awaitItem()

            viewModel.onAction(RegistrationAction.SelectProgram(selectedProgram))
            advanceUntilIdle()
            awaitItem()

            val newPrograms = listOf(Program(id = 3, name = "New", country = "New"))
            programsFlow.value = newPrograms
            advanceUntilIdle()

            val finalState = awaitItem()
            assertThat(finalState.programs).isEqualTo(newPrograms)
            assertThat(finalState.selectedProgram).isEqualTo(selectedProgram)
        }
    }

    // ========================================
    // C. Confirmation
    // ========================================

    @Test
    fun regVm_c01_confirmWithoutSelectionEmitsErrorAndNoEvents() = runTest {
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(RegistrationAction.ConfirmRegistration)
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 0) { deviceCache.saveDevice(any(), any()) }
        coVerify(exactly = 0) { sessionCache.clearSession() }
        coVerify(exactly = 1) { errorMessageEmitter.emit(RegistrationError.PROGRAM_NOT_FOUND, any()) }
    }

    @Test
    fun regVm_c02_repeatedConfirmationsWithoutSelectionEmitMultipleErrors() = runTest {
        repeat(3) {
            viewModel.onAction(RegistrationAction.ConfirmRegistration)
            advanceUntilIdle()
        }

        coVerify(exactly = 3) { errorMessageEmitter.emit(RegistrationError.PROGRAM_NOT_FOUND, any()) }
    }

    @Test
    fun regVm_c03_successfulConfirmationCallsOperationsInOrderAndEmitsEvent() = runTest {
        val selectedProgram = testPrograms[0]
        selectProgram(selectedProgram)

        viewModel.events.test {
            viewModel.onAction(RegistrationAction.ConfirmRegistration)
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isEqualTo(RegistrationEvent.NavigateToLandingScreen)
            expectNoEvents()
        }

        coVerifyOrder {
            deviceCache.saveDevice(match { it.id == -1 }, selectedProgram.id)
            sessionCache.clearSession()
            collectorRepository.upsertCollector(any())
        }
    }

    @Test
    fun regVm_c04_successfulConfirmationCreatesDeviceWithCorrectProperties() = runTest {
        val selectedProgram = testPrograms[0]
        selectProgram(selectedProgram)

        viewModel.events.test {
            viewModel.onAction(RegistrationAction.ConfirmRegistration)
            advanceUntilIdle()
            awaitItem()
        }

        coVerify(exactly = 1) {
            deviceCache.saveDevice(
                device = match<Device> { device ->
                    device.id == -1 &&
                            device.registeredAt > 0 &&
                            device.submittedAt == null &&
                            device.model.isNotEmpty()
                },
                programId = selectedProgram.id
            )
        }
    }

    // ========================================
    // D. Event Emission
    // ========================================

    @Test
    fun regVm_d01_multipleSuccessfulConfirmationsEmitMultipleEvents() = runTest {
        viewModel.state.test {
            awaitItem() 
            awaitItem() 
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.events.test {
            viewModel.onAction(RegistrationAction.SelectProgram(testPrograms[0]))
            advanceUntilIdle()
            viewModel.onAction(RegistrationAction.ConfirmRegistration)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(RegistrationEvent.NavigateToLandingScreen)

            viewModel.onAction(RegistrationAction.SelectProgram(testPrograms[1]))
            advanceUntilIdle()
            viewModel.onAction(RegistrationAction.ConfirmRegistration)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(RegistrationEvent.NavigateToLandingScreen)

            expectNoEvents()
        }
    }

    // ========================================
    // E. Exception Handling
    // ========================================

    @Test
    fun regVm_e01_saveDeviceFailurePreventsClearSessionAndEmitsError() = runTest {
        val selectedProgram = testPrograms[0]
        selectProgram(selectedProgram)

        coEvery { deviceCache.saveDevice(any(), any()) } throws RuntimeException("Save failed")

        viewModel.events.test {
            viewModel.onAction(RegistrationAction.ConfirmRegistration)
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 1) { deviceCache.saveDevice(any(), selectedProgram.id) }
        coVerify(exactly = 0) { sessionCache.clearSession() }
        coVerify(exactly = 0) { collectorRepository.upsertCollector(any()) }
        coVerify(exactly = 1) { errorMessageEmitter.emit(RegistrationError.UNKNOWN_ERROR, any()) }
    }

    @Test
    fun regVm_e02_clearSessionFailureAfterSuccessfulSaveDeviceEmitsError() = runTest {
        val selectedProgram = testPrograms[0]
        selectProgram(selectedProgram)

        coEvery { sessionCache.clearSession() } throws RuntimeException("Clear failed")

        viewModel.events.test {
            viewModel.onAction(RegistrationAction.ConfirmRegistration)
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 1) { deviceCache.saveDevice(any(), selectedProgram.id) }
        coVerify(exactly = 1) { sessionCache.clearSession() }
        coVerify(exactly = 0) { collectorRepository.upsertCollector(any()) }
        coVerify(exactly = 1) { errorMessageEmitter.emit(RegistrationError.UNKNOWN_ERROR, any()) }
    }

    @Test
    fun regVm_e03_differentExceptionTypesAreHandledCorrectly() = runTest {
        val selectedProgram = testPrograms[0]
        selectProgram(selectedProgram)
        coEvery { deviceCache.saveDevice(any(), any()) } throws IllegalStateException("Custom error")

        viewModel.events.test {
            viewModel.onAction(RegistrationAction.ConfirmRegistration)
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify { errorMessageEmitter.emit(RegistrationError.UNKNOWN_ERROR, any()) }
    }

    // ========================================
    // F. Edge Cases
    // ========================================

    @Test
    fun regVm_f01_emptyRepositoryEmitsErrorOnConfirmation() = runTest {
        val emptyFlow = MutableStateFlow(emptyList<Program>())
        every { programRepository.observeAllPrograms() } returns emptyFlow

        val emptyRepoViewModel = RegistrationViewModel(
            transactionHelper = transactionHelper,
            deviceCache = deviceCache,
            currentSessionCache = sessionCache,
            collectorRepository = collectorRepository,
            collectorValidationUseCases = collectorValidationUseCases,
            programDataSource = programDataSource,
            programRepository = programRepository,
            siteDataSource = siteDataSource,
            siteRepository = siteRepository,
            locationTypeDataSource = locationTypeDataSource,
            locationTypeRepository = locationTypeRepository,
            connectivityObserver = connectivityObserver,
            errorMessageEmitter = errorMessageEmitter,
        )

        emptyRepoViewModel.onAction(RegistrationAction.ConfirmRegistration)
        advanceUntilIdle()

        coVerify { errorMessageEmitter.emit(RegistrationError.PROGRAM_NOT_FOUND, any()) }

        emptyRepoViewModel.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun regVm_f02_confirmationWithoutEventCollectorStillCompletesOperations() = runTest {
        val selectedProgram = testPrograms[0]
        selectProgram(selectedProgram)

        viewModel.onAction(RegistrationAction.ConfirmRegistration)
        advanceUntilIdle()

        coVerify(exactly = 1) { deviceCache.saveDevice(any(), selectedProgram.id) }
        coVerify(exactly = 1) { sessionCache.clearSession() }
        coVerify(exactly = 1) { collectorRepository.upsertCollector(any()) } // NEW
    }
}
