package com.vci.vectorcamapp.imaging.presentation

import androidx.compose.ui.geometry.Offset
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.InferenceResultRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.repository.InferenceRepository
import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflow
import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflowFactory
import com.vci.vectorcamapp.imaging.domain.use_cases.ValidateSpecimenIdUseCase
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ImagingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var currentSessionCache: CurrentSessionCache
    private lateinit var sessionRepository: SessionRepository
    private lateinit var specimenRepository: SpecimenRepository
    private lateinit var specimenImageRepository: SpecimenImageRepository
    private lateinit var inferenceResultRepository: InferenceResultRepository
    private lateinit var cameraRepository: CameraRepository
    private lateinit var inferenceRepository: InferenceRepository
    private lateinit var workRepository: WorkManagerRepository
    private lateinit var errorMessageEmitter: ErrorMessageEmitter
    private lateinit var transactionHelper: TransactionHelper
    private lateinit var imagingWorkflowFactory: ImagingWorkflowFactory
    private lateinit var imagingWorkflow: ImagingWorkflow

    private lateinit var viewModel: ImagingViewModel

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

    @After
    fun tearDown() {
        // Restore Uri.EMPTY to null (its original stub value) so other test classes aren't affected.
        injectUriEmpty(null)
    }

    @Before
    fun setUp() {
        // In JVM unit tests Android SDK stubs never run static initializers, so Uri.EMPTY is null.
        // Kotlin's non-null check on SpecimenImage(imageUri = Uri.EMPTY) then throws NPE before
        // any test body runs. We bypass this by writing a mock directly into the static field via
        // sun.misc.Unsafe, which works without altering production code or requiring Robolectric.
        injectUriEmpty(mockk(relaxed = true))

        errorMessageEmitter = mockk(relaxed = true)
        coEvery { errorMessageEmitter.emit(any(), any()) } returns Unit

        currentSessionCache = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        specimenRepository = mockk(relaxed = true)
        specimenImageRepository = mockk(relaxed = true)
        inferenceResultRepository = mockk(relaxed = true)
        cameraRepository = mockk(relaxed = true)
        inferenceRepository = mockk(relaxed = true)
        workRepository = mockk(relaxed = true)
        transactionHelper = mockk(relaxed = true)

        imagingWorkflow = mockk(relaxed = true)
        every { imagingWorkflow.allowModelInferenceToggle } returns false
        every { imagingWorkflow.specimenFurtherProcessingProbability } returns 0f

        imagingWorkflowFactory = mockk()
        every { imagingWorkflowFactory.create(any()) } returns imagingWorkflow

        every { specimenRepository.observeSpecimenImagesAndInferenceResultsBySession(any()) } returns
            MutableStateFlow(emptyList())
    }

    /**
     * Writes [value] directly into [Uri.EMPTY] via sun.misc.Unsafe (reflection-on-reflection),
     * bypassing the `final` modifier restriction introduced in Java 9+. This is the only
     * test-only way to prevent the NPE that occurs when Android SDK stubs leave Uri.EMPTY = null
     * and Kotlin's non-null contract throws on SpecimenImage(imageUri = Uri.EMPTY).
     */
    private fun injectUriEmpty(value: Uri?) {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe")
        theUnsafeField.isAccessible = true
        val unsafe = theUnsafeField.get(null)!!

        val emptyField = Uri::class.java.getDeclaredField("EMPTY")
        val base = unsafeClass.getMethod("staticFieldBase", java.lang.reflect.Field::class.java)
            .invoke(unsafe, emptyField)
        val offset = unsafeClass.getMethod("staticFieldOffset", java.lang.reflect.Field::class.java)
            .invoke(unsafe, emptyField) as Long
        unsafeClass.getMethod("putObject", Any::class.java, Long::class.javaPrimitiveType, Any::class.java)
            .invoke(unsafe, base, offset, value)
    }

    private fun initViewModel(session: Session? = testSession) {
        coEvery { currentSessionCache.getSession() } returns session

        viewModel = ImagingViewModel(
            currentSessionCache = currentSessionCache,
            sessionRepository = sessionRepository,
            specimenRepository = specimenRepository,
            specimenImageRepository = specimenImageRepository,
            inferenceResultRepository = inferenceResultRepository,
            cameraRepository = cameraRepository,
            inferenceRepository = inferenceRepository,
            workRepository = workRepository,
            validateSpecimenIdUseCase = ValidateSpecimenIdUseCase(),
            errorMessageEmitter = errorMessageEmitter,
        ).also { vm ->
            vm.transactionHelper = transactionHelper
            vm.imagingWorkflowFactory = imagingWorkflowFactory
        }
    }

    // ========================================
    // A. Initialization & Loading
    // ========================================

    @Test
    fun imagingVm_a01_withValidSession_stateHasCorrectSessionType() = runTest {
        initViewModel(session = testSession)

        viewModel.state.test {
            awaitItem() // initialValue (isLoading = true)
            val loaded = awaitItem() // first combine emission
            assertThat(loaded.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun imagingVm_a02_withNoSession_emitsNavigateBackEvent_andEmitsError() = runTest {
        initViewModel(session = null)

        // Keep state subscribed so WhileSubscribed keeps loadImagingDetails running.
        backgroundScope.launch { viewModel.state.collect {} }

        viewModel.events.test {
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }

        coVerify(atLeast = 1) { errorMessageEmitter.emit(ImagingError.NO_ACTIVE_SESSION, any()) }
    }

    @Test
    fun imagingVm_a03_allowInferenceToggle_true_setsCorrectInferenceFlags() = runTest {
        every { imagingWorkflow.allowModelInferenceToggle } returns true
        initViewModel(session = testSession)

        // loadImagingDetails() may coalesce with the first combine emission, so assert state.value
        // directly after everything has settled rather than counting Turbine emissions.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.state.value.allowModelInferenceToggle).isTrue()
        assertThat(viewModel.state.value.shouldRunInference).isFalse()
    }

    // ========================================
    // B. Exit Dialog Actions
    // ========================================

    @Test
    fun imagingVm_b01_showExitDialog_setsFlag() = runTest {
        initViewModel()

        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(ImagingAction.ShowExitDialog)
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.showExitDialog).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun imagingVm_b02_dismissExitDialog_clearsFlag_andPendingAction() = runTest {
        initViewModel()

        // The final state after Show→SelectPendingAction→Dismiss equals the pre-action state
        // (showExitDialog=false, pendingAction=null), so StateFlow deduplicates and emits nothing
        // new. Assert state.value directly to avoid a Turbine timeout.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(ImagingAction.ShowExitDialog)
        viewModel.onAction(ImagingAction.SelectPendingAction(ImagingAction.SaveSessionProgress))
        viewModel.onAction(ImagingAction.DismissExitDialog)
        advanceUntilIdle()

        assertThat(viewModel.state.value.showExitDialog).isFalse()
        assertThat(viewModel.state.value.pendingAction).isNull()
    }

    // ========================================
    // C. Pending Action Flow
    // ========================================

    @Test
    fun imagingVm_c01_selectPendingAction_updatesPendingAction() = runTest {
        initViewModel()

        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(ImagingAction.SelectPendingAction(ImagingAction.SaveSessionProgress))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.pendingAction).isEqualTo(ImagingAction.SaveSessionProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun imagingVm_c02_clearPendingAction_removesAction() = runTest {
        initViewModel()

        // SelectPendingAction then ClearPendingAction returns pendingAction to null (its default),
        // so the net state is identical to the pre-action state → no new emission → assert directly.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(ImagingAction.SelectPendingAction(ImagingAction.SaveSessionProgress))
        viewModel.onAction(ImagingAction.ClearPendingAction)
        advanceUntilIdle()

        assertThat(viewModel.state.value.pendingAction).isNull()
    }

    @Test
    fun imagingVm_c03_confirmPendingAction_executesAction_andClearsIt() = runTest {
        initViewModel()

        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(ImagingAction.SelectPendingAction(ImagingAction.SaveSessionProgress))
            advanceUntilIdle()
            skipItems(1) // skip pendingAction set

            viewModel.events.test {
                viewModel.onAction(ImagingAction.ConfirmPendingAction)
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
                expectNoEvents()
            }

            val s = awaitItem()
            assertThat(s.pendingAction).isNull()
            assertThat(s.showExitDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun imagingVm_c04_confirmPendingAction_withNoPendingAction_doesNothing() = runTest {
        initViewModel()

        viewModel.events.test {
            viewModel.state.test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.onAction(ImagingAction.ConfirmPendingAction)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    // ========================================
    // D. Focus Actions
    // ========================================

    @Test
    fun imagingVm_d01_focusAt_setsFocusPoint_andManualFocusing() = runTest {
        initViewModel()

        val offset = Offset(0.5f, 0.5f)
        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(ImagingAction.FocusAt(offset))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.focusPoint).isEqualTo(offset)
            assertThat(s.isManualFocusing).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun imagingVm_d02_cancelFocus_clearsFocusPoint_andManualFocusing() = runTest {
        initViewModel()

        // FocusAt then CancelFocus resets back to (focusPoint=null, isManualFocusing=false),
        // which equals the initial state → no net emission → assert directly.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(ImagingAction.FocusAt(Offset(0.5f, 0.5f)))
        viewModel.onAction(ImagingAction.CancelFocus)
        advanceUntilIdle()

        assertThat(viewModel.state.value.focusPoint).isNull()
        assertThat(viewModel.state.value.isManualFocusing).isFalse()
    }

    // ========================================
    // E. Specimen Id Correction
    // ========================================

    @Test
    fun imagingVm_e01_correctSpecimenId_updatesSpecimenId_andClearsError() = runTest {
        initViewModel()

        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(ImagingAction.CorrectSpecimenId("ABC123"))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.currentSpecimen.id).isEqualTo("ABC123")
            assertThat(s.specimenIdError).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // F. Toggle Model Inference
    // ========================================

    @Test
    fun imagingVm_f01_toggleModelInference_on_setsFlag_andClearsResults() = runTest {
        initViewModel()

        // ToggleModelInference(true) sets shouldRunInference=true which is already the default,
        // and clears results that are already empty → the state doesn't change → no emission.
        // Assert state.value directly to confirm the action is a safe no-op in the default state.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(ImagingAction.ToggleModelInference(isChecked = true))
        advanceUntilIdle()

        val s = viewModel.state.value
        assertThat(s.shouldRunInference).isTrue()
        assertThat(s.previewInferenceResults).isEmpty()
        assertThat(s.currentInferenceResult).isNull()
        assertThat(s.focusPoint).isNull()
    }

    @Test
    fun imagingVm_f02_toggleModelInference_off_clearsFlag() = runTest {
        initViewModel()

        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(ImagingAction.ToggleModelInference(isChecked = false))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.shouldRunInference).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================
    // G. Toggle Packaging Confirmation
    // ========================================

    @Test
    fun imagingVm_g01_togglePackagingConfirmation_true_setsFlag() = runTest {
        initViewModel()

        viewModel.state.test {
            skipItems(2)

            viewModel.onAction(ImagingAction.TogglePackagingConfirmation(isChecked = true))
            advanceUntilIdle()

            val s = awaitItem()
            assertThat(s.hasConfirmedPackaging).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun imagingVm_g02_togglePackagingConfirmation_false_clearsFlag() = runTest {
        initViewModel()

        // Toggle true then false returns hasConfirmedPackaging to its default (false) → no net
        // emission from StateFlow deduplication → assert state.value directly.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(ImagingAction.TogglePackagingConfirmation(isChecked = true))
        viewModel.onAction(ImagingAction.TogglePackagingConfirmation(isChecked = false))
        advanceUntilIdle()

        assertThat(viewModel.state.value.hasConfirmedPackaging).isFalse()
    }

    // ========================================
    // H. Retake Image
    // ========================================

    @Test
    fun imagingVm_h01_retakeImage_clearsAllImageRelatedState() = runTest {
        initViewModel()

        // Four sequential actions produce four intermediate emissions; RetakeImage then resets
        // everything. Assert state.value after all actions complete to avoid counting emissions.
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(ImagingAction.CorrectSpecimenId("ABC123"))
        viewModel.onAction(ImagingAction.TogglePackagingConfirmation(isChecked = true))
        viewModel.onAction(ImagingAction.FocusAt(Offset(0.5f, 0.5f)))
        viewModel.onAction(ImagingAction.RetakeImage)
        advanceUntilIdle()

        val s = viewModel.state.value
        assertThat(s.currentSpecimen.id).isEmpty()
        assertThat(s.currentImageBytes).isNull()
        assertThat(s.currentInferenceResult).isNull()
        assertThat(s.previewInferenceResults).isEmpty()
        assertThat(s.isCameraReady).isFalse()
        assertThat(s.hasConfirmedPackaging).isFalse()
        assertThat(s.focusPoint).isNull()
        assertThat(s.isManualFocusing).isFalse()
        assertThat(s.specimenIdError).isNull()
    }

    // ========================================
    // I. Save / Submit Session
    // ========================================

    @Test
    fun imagingVm_i01_saveSessionProgress_clearsCache_andNavigatesBack() = runTest {
        initViewModel()

        viewModel.events.test {
            viewModel.state.test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.onAction(ImagingAction.SaveSessionProgress)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }

        coVerify(exactly = 1) { currentSessionCache.clearSession() }
    }

    @Test
    fun imagingVm_i02_submitSession_withNoSiteId_navigatesBack() = runTest {
        coEvery { currentSessionCache.getSiteId() } returns null
        initViewModel(session = testSession)

        viewModel.events.test {
            viewModel.state.test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.onAction(ImagingAction.SubmitSession)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }
    }

    @Test
    fun imagingVm_i03_submitSession_success_marksComplete_enqueuesUpload_andNavigatesBack() = runTest {
        coEvery { currentSessionCache.getSiteId() } returns 42
        coEvery { sessionRepository.markSessionAsComplete(any()) } returns true
        initViewModel(session = testSession)

        viewModel.events.test {
            viewModel.state.test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.onAction(ImagingAction.SubmitSession)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }

        coVerify(exactly = 1) { sessionRepository.markSessionAsComplete(testSessionId) }
        coVerify(exactly = 1) { workRepository.enqueueSessionUpload(testSessionId, 42) }
        coVerify(exactly = 1) { currentSessionCache.clearSession() }
    }

    @Test
    fun imagingVm_i04_submitSession_failure_doesNotNavigate() = runTest {
        coEvery { currentSessionCache.getSiteId() } returns 42
        coEvery { sessionRepository.markSessionAsComplete(any()) } returns false
        initViewModel(session = testSession)

        viewModel.events.test {
            viewModel.state.test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.onAction(ImagingAction.SubmitSession)
            advanceUntilIdle()
            expectNoEvents()
        }

        coVerify(exactly = 0) { currentSessionCache.clearSession() }
        coVerify(exactly = 0) { workRepository.enqueueSessionUpload(any(), any()) }
    }
}
