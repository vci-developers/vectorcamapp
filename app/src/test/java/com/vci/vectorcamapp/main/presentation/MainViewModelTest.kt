package com.vci.vectorcamapp.main.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.main.domain.util.MainError
import com.vci.vectorcamapp.main.logging.MainSentryLogger
import com.vci.vectorcamapp.navigation.Destination
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var deviceCache: DeviceCache
    private lateinit var errorMessageEmitter: ErrorMessageEmitter
    private lateinit var viewModel: MainViewModel

    private lateinit var programIdFlow: MutableStateFlow<Int>

    @Before
    fun setUp() {
        deviceCache = mockk(relaxed = true)
        errorMessageEmitter = mockk(relaxed = true)
        coEvery { errorMessageEmitter.emit(any(), any()) } returns Unit
        programIdFlow = MutableStateFlow(-1)
        every { deviceCache.observeProgramId() } returns programIdFlow

        mockkObject(MainSentryLogger)
        coEvery { MainSentryLogger.logDeviceFetchFailure(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(MainSentryLogger)
    }

    // ========================================
    // Test Harness / Helpers
    // ========================================

    private fun initViewModel() {
        viewModel = MainViewModel(
            deviceCache = deviceCache,
            errorMessageEmitter = errorMessageEmitter
        )
    }

    // ========================================
    // A. Initialization & Start Destination
    // ========================================

    @Test
    fun mainVm_a01_programIdExists_setsStartDestinationToLanding() = runTest {
        initViewModel()

        viewModel.state.test {
            val initialState = awaitItem()
            assertThat(initialState.startDestination).isNull()

            programIdFlow.value = 123

            val updatedState = awaitItem()
            assertThat(updatedState.startDestination).isEqualTo(Destination.Landing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun mainVm_a02_programIdMissing_setsStartDestinationToRegistration() = runTest {
        initViewModel()

        viewModel.state.test {
            awaitItem()
            val updatedState = awaitItem()
            assertThat(updatedState.startDestination).isEqualTo(Destination.Registration)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun mainVm_a03_deviceCacheFlow_throwsError_setsDestinationToRegistrationAndLogs() = runTest {
        val testException = RuntimeException("DB error")
        every { deviceCache.observeProgramId() } returns flow { throw testException }

        initViewModel()

        viewModel.state.test {
            awaitItem()
            val errorState = awaitItem()
            assertThat(errorState.startDestination).isEqualTo(Destination.Registration)

            coVerify(exactly = 1) { errorMessageEmitter.emit(MainError.DEVICE_FETCH_FAILED, any()) }
            coVerify(exactly = 1) { MainSentryLogger.logDeviceFetchFailure(any()) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun mainVm_a04_initialState_hasCorrectDefaults() = runTest {
        initViewModel()

        viewModel.state.test {
            val initialState = awaitItem()
            with(initialState) {
                assertThat(startDestination).isNull()
                assertThat(allGranted).isFalse()
                assertThat(isGpsEnabled).isFalse()
                assertThat(permissionChecked).isFalse()
                assertThat(gpsChecked).isFalse()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // B. Action Handling -> Events
    // ========================================

    @Test
    fun mainVm_b01_actions_emitCorrectNavigationEvents() = runTest {
        initViewModel()

        viewModel.events.test {
            viewModel.onAction(MainAction.RequestPermissions)
            assertThat(awaitItem()).isEqualTo(MainEvent.LaunchPermissionRequest)

            viewModel.onAction(MainAction.OpenAppSettings)
            assertThat(awaitItem()).isEqualTo(MainEvent.NavigateToAppSettings)

            viewModel.onAction(MainAction.OpenLocationSettings)
            assertThat(awaitItem()).isEqualTo(MainEvent.NavigateToLocationSettings)

            expectNoEvents()
        }
    }


    // ========================================
    // C. Action Handling -> State Updates
    // ========================================

    @Test
    fun mainVm_c01_updatePermissionStatus_updatesStateCorrectly() = runTest {
        initViewModel()

        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(MainAction.UpdatePermissionStatus(allGranted = true))

            val stateAfterGrant = awaitItem()
            assertThat(stateAfterGrant.allGranted).isTrue()
            assertThat(stateAfterGrant.permissionChecked).isTrue()

            viewModel.onAction(MainAction.UpdatePermissionStatus(allGranted = false))

            val stateAfterDeny = awaitItem()
            assertThat(stateAfterDeny.allGranted).isFalse()
            assertThat(stateAfterDeny.permissionChecked).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun mainVm_c02_updateGpsStatus_updatesStateCorrectly() = runTest {
        initViewModel()

        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(MainAction.UpdateGpsStatus(isGpsEnabled = true))

            val stateAfterEnable = awaitItem()
            assertThat(stateAfterEnable.isGpsEnabled).isTrue()
            assertThat(stateAfterEnable.gpsChecked).isTrue()

            viewModel.onAction(MainAction.UpdateGpsStatus(isGpsEnabled = false))

            val stateAfterDisable = awaitItem()
            assertThat(stateAfterDisable.isGpsEnabled).isFalse()
            assertThat(stateAfterDisable.gpsChecked).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // D. Edge Cases
    // ========================================

    @Test
    fun mainVm_d01_programIdChanges_updatesDestinationDynamically() = runTest {
        initViewModel()

        viewModel.state.test {
            skipItems(2)

            programIdFlow.value = 123
            val landingState = awaitItem()
            assertThat(landingState.startDestination).isEqualTo(Destination.Landing)

            programIdFlow.value = -1
            val registrationState = awaitItem()
            assertThat(registrationState.startDestination).isEqualTo(Destination.Registration)

            cancelAndIgnoreRemainingEvents()
        }
    }
}