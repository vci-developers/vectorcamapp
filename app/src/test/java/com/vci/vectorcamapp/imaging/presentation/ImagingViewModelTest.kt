package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.core.net.toUri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenImageAndInferenceResult
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.repository.InferenceResultRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageBus
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.imaging.domain.model.CapturedFrameProcessingResult
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflow
import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflowFactory
import com.vci.vectorcamapp.imaging.domain.use_cases.ValidateSpecimenIdUseCase
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
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
class ImagingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionCache: CurrentSessionCache
    private lateinit var sessionRepository: SessionRepository
    private lateinit var specimenRepository: SpecimenRepository
    private lateinit var specimenImageRepository: SpecimenImageRepository
    private lateinit var inferenceRepository: InferenceResultRepository
    private lateinit var cameraRepository: CameraRepository
    private lateinit var workRepository: WorkManagerRepository
    private lateinit var validateId: ValidateSpecimenIdUseCase
    private lateinit var transactionHelper: TransactionHelper
    private lateinit var workflowFactory: ImagingWorkflowFactory
    private lateinit var workflow: ImagingWorkflow

    private lateinit var viewModel: ImagingViewModel

    private lateinit var compositesFlow: MutableStateFlow<List<SpecimenWithSpecimenImagesAndInferenceResults>>

    private lateinit var fakeUri: Uri

    private fun newViewModel(): ImagingViewModel = ImagingViewModel(
        currentSessionCache = sessionCache,
        sessionRepository = sessionRepository,
        specimenRepository = specimenRepository,
        specimenImageRepository = specimenImageRepository,
        inferenceResultRepository = inferenceRepository,
        cameraRepository = cameraRepository,
        workRepository = workRepository,
        validateSpecimenIdUseCase = validateId
    ).also {
        it.transactionHelper = transactionHelper
        it.imagingWorkflowFactory = workflowFactory
    }

    private fun fakeComposite(id: String) =
        SpecimenWithSpecimenImagesAndInferenceResults(
            specimen = Specimen(id = id, remoteId = null),
            specimenImagesAndInferenceResults = listOf(
                SpecimenImageAndInferenceResult(
                    specimenImage = SpecimenImage(
                        localId = "img-$id",
                        remoteId = null,
                        species = "Anopheles",
                        sex = "F",
                        abdomenStatus = "Fed",
                        imageUri = "".toUri(),
                        imageUploadStatus = UploadStatus.NOT_STARTED,
                        metadataUploadStatus = UploadStatus.NOT_STARTED,
                        capturedAt = 0L,
                        submittedAt = null
                    ),
                    inferenceResult = null
                )
            )
        )

    private fun mutateVmState(mutator: (ImagingState) -> ImagingState) {
        val field = ImagingViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val mutable = field.get(viewModel) as MutableStateFlow<ImagingState>
        mutable.value = mutator(mutable.value)
    }

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        fakeUri = mockk(relaxed = true)
        every { Uri.parse(any()) } returns fakeUri
        mockkObject(ErrorMessageBus)
        coEvery { ErrorMessageBus.emit(any(), any()) } returns Unit

        sessionCache = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        specimenRepository = mockk(relaxed = true)
        specimenImageRepository = mockk(relaxed = true)
        inferenceRepository = mockk(relaxed = true)
        cameraRepository = mockk(relaxed = true)
        workRepository = mockk(relaxed = true)
        validateId = mockk(relaxed = true)
        transactionHelper = mockk(relaxed = true)
        workflowFactory = mockk()
        workflow = mockk(relaxed = true)

        coEvery { sessionCache.getSession() } returns null

        compositesFlow = MutableStateFlow(emptyList())
        every { specimenRepository.observeSpecimenImagesAndInferenceResultsBySession(any()) } returns compositesFlow

        every { workflowFactory.create(any()) } returns workflow

        viewModel = newViewModel()
    }

    @After
    fun tearDown() {
        unmockkObject(ErrorMessageBus)
        unmockkStatic(Uri::class)
    }

    // ========================================
    // A. Initialization / state from repository
    // ========================================

    @Test
    fun imgVm_a01_noSessionOnInit_navigatesAndStopsLoading() = runTest {
        viewModel.state.test {
            val s0 = awaitItem()
            assertThat(s0.isLoading).isTrue()

            val s1 = awaitItem()
            assertThat(s1.isLoading).isFalse()
            assertThat(s1.specimensWithImagesAndInferenceResults).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.events.test {
            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }
    }

    @Test
    fun imgVm_a02_sessionPresent_stateReflectsRepoFlow_and_workflowCreated() = runTest {
        val sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val session = mockk<Session>()
        every { session.localId } returns sessionId
        val sessionType = mockk<SessionType>()
        every { session.type } returns sessionType

        coEvery { sessionCache.getSession() } returns session
        viewModel = newViewModel()

        val list1 = listOf(fakeComposite("S-001"))
        val list2 = listOf(fakeComposite("S-001"), fakeComposite("S-002"))

        viewModel.state.test {
            val s0 = awaitItem()
            assertThat(s0.isLoading).isTrue()

            val s1 = awaitItem()
            assertThat(s1.isLoading).isFalse()
            assertThat(s1.specimensWithImagesAndInferenceResults).isEmpty()

            compositesFlow.value = list1
            advanceUntilIdle()
            val s2 = awaitItem()
            assertThat(s2.specimensWithImagesAndInferenceResults).hasSize(1)

            compositesFlow.value = list2
            advanceUntilIdle()
            val s3 = awaitItem()
            assertThat(s3.specimensWithImagesAndInferenceResults).hasSize(2)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { workflowFactory.create(sessionType) }
    }

    // ========================================
    // B. Lightweight state actions
    // ========================================

    @Test
    fun imgVm_b01_manualFocus_updatesAndClearsPoint() = runTest {
        val p = androidx.compose.ui.geometry.Offset(10f, 20f)

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(ImagingAction.FocusAt(p))
            advanceUntilIdle()
            assertThat(awaitItem().focusPoint).isEqualTo(p)

            viewModel.onAction(ImagingAction.CancelFocus)
            advanceUntilIdle()
            assertThat(awaitItem().focusPoint).isNull()
        }
    }

    @Test
    fun imgVm_b02_correctSpecimenId_updatesCurrentSpecimen() = runTest {
        viewModel.state.test {
            awaitItem()
            viewModel.onAction(ImagingAction.CorrectSpecimenId("ABC-123"))
            advanceUntilIdle()
            assertThat(awaitItem().currentSpecimen.id).isEqualTo("ABC-123")
        }
    }

    @Test
    fun imgVm_b03_retakeImage_resetsTransientFields() = runTest {
        viewModel.onAction(ImagingAction.CorrectSpecimenId("TEMP"))
        advanceUntilIdle()

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(ImagingAction.RetakeImage)
            advanceUntilIdle()
            val s = awaitItem()
            assertThat(s.currentSpecimen.id).isEmpty()
            assertThat(s.currentImageBytes).isNull()
            assertThat(s.currentInferenceResult).isNull()
            assertThat(s.previewInferenceResults).isEmpty()
            assertThat(s.isCameraReady).isFalse()
        }
    }

    // ========================================
    // C. Exit / navigation flows
    // ========================================

    @Test
    fun imgVm_c01_saveSessionProgress_clearsSession_and_navigates() = runTest {
        viewModel.events.test {
            viewModel.onAction(ImagingAction.SaveSessionProgress)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }
        coVerify(exactly = 1) { sessionCache.clearSession() }
    }

    @Test
    fun imgVm_c02_submitSession_withoutSession_navigatesOnly() = runTest {
        viewModel.events.test {
            viewModel.onAction(ImagingAction.SubmitSession)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }
        coVerify(exactly = 0) { sessionRepository.markSessionAsComplete(any()) }
        coVerify(exactly = 0) { workRepository.enqueueSessionUpload(any(), any()) }
        coVerify(exactly = 0) { sessionCache.clearSession() }
    }

    @Test
    fun imgVm_c03_submitSession_success_marksComplete_enqueuesUpload_clears_and_navigates() = runTest {
        val sessionId = UUID.fromString("00000000-0000-0000-0000-00000000002a")
        val session = mockk<Session>()
        every { session.localId } returns sessionId
        val sessionType = mockk<SessionType>()
        every { session.type } returns sessionType

        coEvery { sessionCache.getSession() } returns session
        coEvery { sessionCache.getSiteId() } returns 9
        coEvery { sessionRepository.markSessionAsComplete(sessionId) } returns true

        viewModel = newViewModel()

        viewModel.events.test {
            viewModel.onAction(ImagingAction.SubmitSession)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }

        coVerifyOrder {
            sessionRepository.markSessionAsComplete(sessionId)
            workRepository.enqueueSessionUpload(sessionId, 9)
            sessionCache.clearSession()
        }
    }

    @Test
    fun imgVM_c04_showExitDialog_sets_ShowExitDialog() = runTest {
        val session = mockk<Session>()
        every { session.localId } returns UUID.fromString("00000000-0000-0000-0000-000000000101")
        every { session.type } returns mockk<SessionType>()
        coEvery { sessionCache.getSession() } returns session

        viewModel = newViewModel()

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(ImagingAction.ShowExitDialog)

            val after = awaitItem()
            assertThat(after.showExitDialog).isTrue()
        }

        viewModel.events.test {
            viewModel.onAction(ImagingAction.ShowExitDialog)
            expectNoEvents()
        }
    }

    @Test
    fun imgVM_c05_DismissExitDialog_sets_ShowExitDialog_and_PendingAction() = runTest {
        val session = mockk<Session>()
        every { session.localId } returns UUID.fromString("00000000-0000-0000-0000-000000000101")
        every { session.type } returns mockk<SessionType>()
        coEvery { sessionCache.getSession() } returns session

        viewModel = newViewModel()

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(ImagingAction.DismissExitDialog)

            val after = awaitItem()
            assertThat(after.showExitDialog).isFalse()
            assertThat(after.pendingAction).isNull()
        }

        viewModel.events.test {
            viewModel.onAction(ImagingAction.ShowExitDialog)
            expectNoEvents()
        }
    }

    @Test
    fun imgVM_c06_SelectPendingAction_sets_PendingAction() = runTest {
        val session = mockk<Session>()
        every { session.localId } returns UUID.fromString("00000000-0000-0000-0000-000000000101")
        every { session.type } returns mockk<SessionType>()
        coEvery { sessionCache.getSession() } returns session

        viewModel = newViewModel()

        viewModel.state.test {
            awaitItem()
            viewModel.onAction(ImagingAction.SelectPendingAction(ImagingAction.SaveImageToSession))

            val after = awaitItem()
            assertThat(after.pendingAction).isEqualTo(ImagingAction.SaveImageToSession)
        }

        viewModel.events.test {
            viewModel.onAction(ImagingAction.ShowExitDialog)
            expectNoEvents()
        }
    }

    @Test
    fun imgVM_c07_confirmPendingAction_SaveSessionProgress_executes_and_navigates() = runTest {
        val session = mockk<Session>()
        every { session.localId } returns UUID.fromString("00000000-0000-0000-0000-000000000101")
        every { session.type } returns mockk<SessionType>()
        coEvery { sessionCache.getSession() } returns session
        viewModel = newViewModel()

        viewModel.events.test {
            viewModel.onAction(ImagingAction.SelectPendingAction(ImagingAction.SaveSessionProgress))
            viewModel.onAction(ImagingAction.ConfirmPendingAction)

            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }

        viewModel.state.test {
            awaitItem()
            val after = awaitItem()
            assertThat(after.pendingAction).isNull()
        }

        coVerify(exactly = 1) { sessionCache.clearSession() }
    }

    @Test
    fun imgVM_c08_confirmPendingAction_SubmitSession_marksComplete_enqueues_and_navigates() = runTest {
        val sessionId = UUID.fromString("00000000-0000-0000-0000-00000000002a")
        val session = mockk<Session>()
        every { session.localId } returns sessionId
        every { session.type } returns mockk<SessionType>()
        coEvery { sessionCache.getSession() } returns session
        coEvery { sessionCache.getSiteId() } returns 9
        coEvery { sessionRepository.markSessionAsComplete(sessionId) } returns true
        viewModel = newViewModel()

        viewModel.events.test {
            viewModel.onAction(ImagingAction.SelectPendingAction(ImagingAction.SubmitSession))
            viewModel.onAction(ImagingAction.ConfirmPendingAction)

            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }

        viewModel.state.test {
            awaitItem()
            val after = awaitItem()
            assertThat(after.pendingAction).isNull()
        }

        coVerifyOrder {
            sessionRepository.markSessionAsComplete(sessionId)
            workRepository.enqueueSessionUpload(sessionId, 9)
            sessionCache.clearSession()
        }
    }

    // ========================================
    // D. SaveImageToSession guard rails
    // ========================================

    @Test
    fun imgVm_d01_saveImageToSession_withoutSession_navigatesBack() = runTest {
        coEvery { sessionCache.getSession() } returns null
        viewModel = newViewModel()

        viewModel.events.test {
            viewModel.onAction(ImagingAction.SaveImageToSession)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            expectNoEvents()
        }
    }

    @Test
    fun imgVm_d02_saveImageToSession_invalidSpecimenId_emitsError_and_returnsEarly() = runTest {
        val sessionId = UUID.fromString("00000000-0000-0000-0000-00000000abcd")
        val session = mockk<Session>()
        every { session.localId } returns sessionId
        val sessionType = mockk<SessionType>()
        every { session.type } returns sessionType
        coEvery { sessionCache.getSession() } returns session

        coEvery { validateId(any(), shouldAutoCorrect = false) } returns Result.Error(ImagingError.INVALID_SPECIMEN_ID)

        viewModel = newViewModel()

        viewModel.onAction(ImagingAction.CorrectSpecimenId("bad-id"))
        advanceUntilIdle()

        viewModel.onAction(ImagingAction.SaveImageToSession)
        advanceUntilIdle()

        coVerify(exactly = 1) { validateId("bad-id", shouldAutoCorrect = false) }
        coVerify(exactly = 1) { ErrorMessageBus.emit(ImagingError.INVALID_SPECIMEN_ID, any()) }
        coVerify(exactly = 0) { cameraRepository.saveImage(any(), any(), any()) }
        coVerify(exactly = 0) { specimenImageRepository.insertSpecimenImage(any(), any(), any()) }
    }

    // ========================================
    // E. Image Capture Flow
    // ========================================

    @Test
    fun imgVm_e01_captureImage_ignored_when_cameraNotReady() = runTest {
        viewModel.onAction(ImagingAction.CaptureImage(controller = mockk(relaxed = true)))
        advanceUntilIdle()
        coVerify(exactly = 0) { cameraRepository.captureImage(any()) }
    }

    @Test
    fun imgVm_e02_captureImage_success_updatesStateWithProcessingResult() = runTest {
        val session = mockk<Session>()
        every { session.localId } returns UUID.randomUUID()
        every { session.type } returns mockk<SessionType>()
        coEvery { sessionCache.getSession() } returns session
        viewModel = newViewModel()

        val mockImageProxy: ImageProxy = mockk(relaxed = true)
        val mockInputBitmap: Bitmap = mockk(relaxed = true)
        val mockDecodedBitmap: Bitmap = mockk(relaxed = true)

        mockkStatic("com.vci.vectorcamapp.imaging.presentation.extensions.ImageProxyExtensionsKt")
        every { mockImageProxy.toUprightBitmap() } returns mockInputBitmap

        every {
            mockInputBitmap.compress(any(), any(), any<java.io.OutputStream>())
        } answers {
            val stream = arg<java.io.OutputStream>(2)
            stream.write(byteArrayOf(1, 2, 3))
            true
        }

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns mockDecodedBitmap

        coEvery { cameraRepository.captureImage(any()) } returns Result.Success(mockImageProxy)

        val processingResult = CapturedFrameProcessingResult(
            species = "Anopheles",
            sex = "F",
            abdomenStatus = "Fed",
            capturedInferenceResult = InferenceResult(
                bboxTopLeftX = 0.1f,
                bboxTopLeftY = 0.2f,
                bboxWidth = 0.3f,
                bboxHeight = 0.4f,
                bboxConfidence = 0.9f,
                bboxClassId = 0,
                speciesLogits = floatArrayOf(0.9f, 0.1f).toList(),
                sexLogits = floatArrayOf(0.8f, 0.2f).toList(),
                abdomenStatusLogits = floatArrayOf(0.7f, 0.3f).toList(),
                speciesInferenceDuration = 100,
                sexInferenceDuration = 50,
                abdomenStatusInferenceDuration = 75
            )
        )
        coEvery { workflow.processCapturedFrame(mockDecodedBitmap) } returns Result.Success(processingResult)

        mutateVmState { it.copy(isCameraReady = true) }

        advanceUntilIdle()

        viewModel.state.test {
            var s0 = awaitItem()
            while (!s0.isCameraReady) {
                s0 = awaitItem()
            }

            assertThat(s0.isProcessing).isFalse()

            viewModel.onAction(ImagingAction.CaptureImage(mockk(relaxed = true)))

            var finalState = awaitItem()
            if (finalState.isProcessing) {
                finalState = awaitItem()
            }

            assertThat(finalState.currentImageBytes).isNotNull()
            assertThat(finalState.currentSpecimenImage.species).isEqualTo("Anopheles")
            assertThat(finalState.currentSpecimenImage.sex).isEqualTo("F")
            assertThat(finalState.currentSpecimenImage.abdomenStatus).isEqualTo("Fed")
            assertThat(finalState.currentInferenceResult).isEqualTo(processingResult.capturedInferenceResult)
            assertThat(finalState.previewInferenceResults).isEmpty()

            cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 1) { mockImageProxy.close() }
        unmockkStatic("com.vci.vectorcamapp.imaging.presentation.extensions.ImageProxyExtensionsKt")
        unmockkStatic(BitmapFactory::class)
    }
}
