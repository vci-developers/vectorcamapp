package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.InferenceResultRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import com.vci.vectorcamapp.core.logging.analytics.VectorCamAnalytics
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.rules.MainDispatcherRule
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.repository.InferenceRepository
import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflow
import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflowFactory
import com.vci.vectorcamapp.imaging.domain.use_cases.ValidateSpecimenIdUseCase
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import com.vci.vectorcamapp.imaging.presentation.enums.CaptureStage
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import com.vci.vectorcamapp.navigation.Destination
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.OutputStream
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ImagingViewModelBranchTest {

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
    private lateinit var imagingWorkflow: ImagingWorkflow
    private lateinit var viewModel: ImagingViewModel

    private val sessionId = UUID.fromString("00000000-0000-0000-0000-0000000000aa")
    private val session = Session(
        localId = sessionId,
        remoteId = null,
        hardwareId = "HW",
        collectorTitle = "Dr.",
        collectorName = "Ada",
        collectorLastTrainedOn = 0L,
        collectionDate = 1_000L,
        collectionMethod = "Net",
        specimenCondition = "Fresh",
        createdAt = 1_000L,
        completedAt = null,
        submittedAt = null,
        notes = "",
        latitude = null,
        longitude = null,
        type = SessionType.SURVEILLANCE,
    )

    private val uprightBitmap = mockk<Bitmap>(relaxed = true)

    @Before
    fun setUp() {
        injectUriEmpty(mockk(relaxed = true))
        VectorCamAnalytics.analytics = null
        every { uprightBitmap.width } returns 20
        every { uprightBitmap.height } returns 10
        every { uprightBitmap.compress(any(), any(), any()) } answers {
            thirdArg<OutputStream>().write(byteArrayOf(7, 7, 7))
            true
        }

        currentSessionCache = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        specimenRepository = mockk(relaxed = true)
        specimenImageRepository = mockk(relaxed = true)
        inferenceResultRepository = mockk(relaxed = true)
        cameraRepository = mockk(relaxed = true)
        inferenceRepository = mockk(relaxed = true)
        workRepository = mockk(relaxed = true)
        errorMessageEmitter = mockk(relaxed = true)
        transactionHelper = mockk(relaxed = true)
        imagingWorkflow = mockk(relaxed = true)
        every { imagingWorkflow.allowModelInferenceToggle } returns false
        every { imagingWorkflow.specimenFurtherProcessingProbability } returns 0f
        val factory = mockk<ImagingWorkflowFactory>()
        every { factory.create(any()) } returns imagingWorkflow
        every {
            specimenRepository.observeSpecimenImagesAndInferenceResultsBySessionScope(any(), any())
        } returns MutableStateFlow(emptyList())
        coEvery { currentSessionCache.getSession() } returns session

        val savedStateHandle = mockk<SavedStateHandle>(relaxed = true)
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every { savedStateHandle.toRoute<Destination.Imaging>() } returns Destination.Imaging(null)
        viewModel = ImagingViewModel(
            savedStateHandle = savedStateHandle,
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
        ).also {
            it.transactionHelper = transactionHelper
            it.imagingWorkflowFactory = factory
        }
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        runCatching { unmockkStatic(BITMAP_EXTENSION) }
        injectUriEmpty(null)
    }

    @Test
    fun processFrame_validIdWithoutInference_updatesSpecimenId() = runTest {
        collectState()
        viewModel.onAction(ImagingAction.ToggleModelInference(false))
        advanceUntilIdle()
        val frame = frameReturning(uprightBitmap)
        coEvery { inferenceRepository.readSpecimenId(uprightBitmap) } returns "abc123"

        viewModel.onAction(ImagingAction.ProcessFrame(frame))
        advanceUntilIdle()

        assertThat(viewModel.state.value.isCameraReady).isTrue()
        assertThat(viewModel.state.value.currentSpecimen.id).isEqualTo("ABC123")
        io.mockk.verify { frame.close() }
    }

    @Test
    fun processFrame_invalidId_clearsSpecimenId() = runTest {
        collectState()
        viewModel.onAction(ImagingAction.ToggleModelInference(false))
        advanceUntilIdle()
        val frame = frameReturning(uprightBitmap)
        coEvery { inferenceRepository.readSpecimenId(uprightBitmap) } returns "nope"

        viewModel.onAction(ImagingAction.ProcessFrame(frame))
        advanceUntilIdle()

        assertThat(viewModel.state.value.currentSpecimen.id).isEmpty()
    }

    @Test
    fun processFrame_failure_emitsProcessingErrorAndThrottlesRepeatLog() = runTest {
        collectState()
        mockkStatic(BITMAP_EXTENSION)
        val frame = mockk<ImageProxy>(relaxed = true)
        every { frame.toUprightBitmap() } throws IllegalStateException("decode")

        viewModel.onAction(ImagingAction.ProcessFrame(frame))
        viewModel.onAction(ImagingAction.ProcessFrame(frame))
        advanceUntilIdle()

        coVerify(atLeast = 2) { errorMessageEmitter.emit(ImagingError.PROCESSING_ERROR, any()) }
    }

    @Test
    fun processFrame_whileCapturing_closesFrameWithoutReadingId() = runTest {
        collectState()
        seed(captureStage = CaptureStage.CAPTURING, isCameraReady = true)
        mockkStatic(BITMAP_EXTENSION)
        val frame = mockk<ImageProxy>(relaxed = true)
        every { frame.toUprightBitmap() } throws AssertionError("frame should not be decoded")

        viewModel.onAction(ImagingAction.ProcessFrame(frame))
        advanceUntilIdle()

        coVerify(exactly = 0) { inferenceRepository.readSpecimenId(any()) }
        io.mockk.verify { frame.close() }
    }

    @Test
    fun save_withoutSession_navigatesBack() = runTest {
        collectState()
        coEvery { currentSessionCache.getSession() } returns null

        viewModel.events.test {
            viewModel.onAction(ImagingAction.SaveImageToSession)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ImagingEvent.NavigateBackToLandingScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun save_invalidId_emitsError() = runTest {
        collectState()
        seed(specimenId = "BAD")

        viewModel.onAction(ImagingAction.SaveImageToSession)
        advanceUntilIdle()

        assertThat(viewModel.state.value.specimenIdError).isEqualTo(ImagingError.INVALID_SPECIMEN_ID)
        coVerify(exactly = 0) { cameraRepository.saveImage(any(), any(), any()) }
    }

    @Test
    fun save_withoutImageBytes_doesNotSave() = runTest {
        collectState()
        seed(specimenId = "ABC123", imageBytes = null)

        viewModel.onAction(ImagingAction.SaveImageToSession)
        advanceUntilIdle()

        coVerify(exactly = 0) { cameraRepository.saveImage(any(), any(), any()) }
    }

    @Test
    fun save_newSpecimen_persistsAndClearsCapture() = runTest {
        collectState()
        seed(specimenId = "ABC123", inferenceResult = inferenceResult())
        stubSave(insertSpecimen = Result.Success(Unit))

        viewModel.onAction(ImagingAction.SaveImageToSession)
        advanceUntilIdle()

        assertThat(viewModel.state.value.currentImageBytes).isNull()
        coVerify { specimenRepository.insertSpecimen(any(), sessionId, null) }
        coVerify { inferenceResultRepository.insertInferenceResult(any(), any()) }
    }

    @Test
    fun save_requestedForFurtherProcessing_waitsForPackagingConfirmation() = runTest {
        collectState()
        seed(specimenId = "ABC123", shouldProcessFurther = true)

        viewModel.onAction(ImagingAction.SaveImageToSession)
        advanceUntilIdle()

        assertThat(viewModel.state.value.currentSpecimen.shouldProcessFurther).isTrue()
        coVerify(exactly = 0) { cameraRepository.saveImage(any(), any(), any()) }
    }

    @Test
    fun save_monthlyCapReached_skipsFurtherProcessingAndSaves() = runTest {
        collectState()
        every { imagingWorkflow.specimenFurtherProcessingProbability } returns 1f
        coEvery {
            specimenRepository.countSelectedForFurtherProcessingBetweenSessionCollectionDates(any(), any())
        } returns 20
        seed(specimenId = "ABC123")
        stubSave(insertSpecimen = Result.Success(Unit))

        viewModel.onAction(ImagingAction.SaveImageToSession)
        advanceUntilIdle()

        assertThat(viewModel.state.value.currentSpecimen.shouldProcessFurther).isFalse()
        coVerify { cameraRepository.saveImage(any(), any(), session) }
    }

    @Test
    fun save_insertFailure_deletesSavedImage() = runTest {
        collectState()
        seed(specimenId = "ABC123")
        stubSave(insertSpecimen = Result.Error(RoomDbError.CONSTRAINT_VIOLATION))

        viewModel.onAction(ImagingAction.SaveImageToSession)
        advanceUntilIdle()

        coVerify { errorMessageEmitter.emit(ImagingError.SAVE_ERROR, any()) }
        coVerify { cameraRepository.deleteSavedImage(any()) }
    }

    @Test
    fun save_repositoryError_emitsThatError() = runTest {
        collectState()
        seed(specimenId = "ABC123")
        coEvery { specimenRepository.getSpecimenByIdAndSessionId(any(), any()) } returns null
        coEvery { cameraRepository.saveImage(any(), any(), any()) } returns Result.Error(ImagingError.SAVE_ERROR)

        viewModel.onAction(ImagingAction.SaveImageToSession)
        advanceUntilIdle()

        coVerify { errorMessageEmitter.emit(ImagingError.SAVE_ERROR, any()) }
    }

    @Test
    fun returnToCollectionBatchList_emitsSessionNavigation() = runTest {
        collectState()

        viewModel.events.test {
            viewModel.onAction(ImagingAction.ReturnToCollectionBatchList)
            advanceUntilIdle()
            assertThat(awaitItem())
                .isEqualTo(ImagingEvent.NavigateBackToCollectionBatchListScreen(sessionId))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun focusAt_repeatedQuickly_keepsThePoint() = runTest {
        collectState()
        val point = Offset(0.1f, 0.2f)

        viewModel.onAction(ImagingAction.FocusAt(point))
        viewModel.onAction(ImagingAction.FocusAt(Offset(0.3f, 0.4f)))
        advanceUntilIdle()

        assertThat(viewModel.state.value.focusPoint).isEqualTo(Offset(0.3f, 0.4f))
        assertThat(viewModel.state.value.isManualFocusing).isTrue()
    }

    private fun TestScope.collectState() {
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
    }

    private fun frameReturning(bitmap: Bitmap): ImageProxy {
        mockkStatic(BITMAP_EXTENSION)
        val frame = mockk<ImageProxy>(relaxed = true)
        every { frame.toUprightBitmap() } returns bitmap
        return frame
    }

    private fun stubSave(insertSpecimen: Result<Unit, RoomDbError>) {
        val uri = mockk<Uri>(relaxed = true)
        coEvery { specimenRepository.getSpecimenByIdAndSessionId(any(), any()) } returns null
        coEvery { cameraRepository.saveImage(any(), any(), any()) } returns Result.Success(uri)
        coEvery { transactionHelper.runAsTransaction<Boolean>(any()) } coAnswers {
            firstArg<suspend () -> Boolean>().invoke()
        }
        coEvery { specimenRepository.insertSpecimen(any(), any(), any()) } returns insertSpecimen
        coEvery {
            specimenImageRepository.insertSpecimenImage(any(), any(), any())
        } returns Result.Success(Unit)
        coEvery {
            inferenceResultRepository.insertInferenceResult(any(), any())
        } returns Result.Success(Unit)
    }

    private fun seed(
        specimenId: String = "",
        imageBytes: ByteArray? = byteArrayOf(1, 2, 3),
        shouldProcessFurther: Boolean = false,
        captureStage: CaptureStage? = null,
        isCameraReady: Boolean = false,
        inferenceResult: InferenceResult? = null,
    ) {
        val field = ImagingViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<ImagingState>
        stateFlow.value = stateFlow.value.copy(
            currentSpecimen = stateFlow.value.currentSpecimen.copy(
                id = specimenId,
                shouldProcessFurther = shouldProcessFurther,
            ),
            currentImageBytes = imageBytes,
            captureStage = captureStage,
            isCameraReady = isCameraReady,
            currentInferenceResult = inferenceResult,
        )
    }

    private fun inferenceResult() = InferenceResult(
        bboxTopLeftX = 0f,
        bboxTopLeftY = 0f,
        bboxWidth = 1f,
        bboxHeight = 1f,
        bboxConfidence = 0.5f,
        bboxClassId = 0,
        speciesLogits = null,
        sexLogits = null,
        abdomenStatusLogits = null,
        bboxDetectionDuration = 1L,
        speciesInferenceDuration = null,
        sexInferenceDuration = null,
        abdomenStatusInferenceDuration = null,
    )

    private fun injectUriEmpty(value: Uri?) {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe")
        theUnsafeField.isAccessible = true
        val unsafe = theUnsafeField.get(null)
        val emptyField = Uri::class.java.getDeclaredField("EMPTY")
        val base = unsafeClass.getMethod("staticFieldBase", java.lang.reflect.Field::class.java)
            .invoke(unsafe, emptyField)
        val offset = unsafeClass.getMethod("staticFieldOffset", java.lang.reflect.Field::class.java)
            .invoke(unsafe, emptyField) as Long
        unsafeClass.getMethod(
            "putObject",
            Any::class.java,
            Long::class.javaPrimitiveType,
            Any::class.java,
        ).invoke(unsafe, base, offset, value)
    }

    private companion object {
        const val BITMAP_EXTENSION =
            "com.vci.vectorcamapp.imaging.presentation.extensions.ImageProxyExtensionsKt"
    }
}
