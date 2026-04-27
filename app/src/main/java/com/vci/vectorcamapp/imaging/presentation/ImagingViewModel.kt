package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import android.os.SystemClock
import android.net.Uri
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.repository.InferenceResultRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.imaging.domain.enums.AbdomenStatusLabel
import com.vci.vectorcamapp.imaging.domain.enums.SexLabel
import com.vci.vectorcamapp.imaging.domain.enums.SpeciesLabel
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.repository.InferenceRepository
import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflow
import com.vci.vectorcamapp.imaging.domain.strategy.ImagingWorkflowFactory
import com.vci.vectorcamapp.imaging.domain.use_cases.ValidateSpecimenIdUseCase
import com.vci.vectorcamapp.imaging.domain.model.AfRegion
import com.vci.vectorcamapp.imaging.domain.model.CameraMetadata
import com.vci.vectorcamapp.imaging.domain.model.ColorCorrectionGains
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random
import androidx.core.graphics.createBitmap
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Mat

@HiltViewModel
class ImagingViewModel @Inject constructor(
    private val currentSessionCache: CurrentSessionCache,
    private val sessionRepository: SessionRepository,
    private val specimenRepository: SpecimenRepository,
    private val specimenImageRepository: SpecimenImageRepository,
    private val inferenceResultRepository: InferenceResultRepository,
    private val cameraRepository: CameraRepository,
    private val inferenceRepository: InferenceRepository,
    private val workRepository: WorkManagerRepository,
    private val validateSpecimenIdUseCase: ValidateSpecimenIdUseCase,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

    private val processFrameMutex = Mutex()

    private var lastAutoCaptureBbox: InferenceResult? = null
    private var autoCaptureStableFrameCount: Int = 0
    /** [SystemClock.elapsedRealtime] when the current stable streak began; 0 = unset. */
    private var autoCaptureStableStreakStartElapsedMs: Long = 0L

    @Inject
    lateinit var transactionHelper: TransactionHelper

    @Inject
    lateinit var imagingWorkflowFactory: ImagingWorkflowFactory
    private lateinit var imagingWorkflow: ImagingWorkflow

    private val _specimensWithImagesAndInferenceResults: Flow<List<SpecimenWithSpecimenImagesAndInferenceResults>> =
        flow {
            val session = currentSessionCache.getSession()
            if (session == null) {
                emit(emptyList())
            } else {
                specimenRepository.observeSpecimenImagesAndInferenceResultsBySession(session.localId)
                    .collect { emit(it) }
            }
        }

    private val _state = MutableStateFlow(ImagingState())
    val state: StateFlow<ImagingState> = combine(
        _specimensWithImagesAndInferenceResults, _state
    ) { specimensWithImagesAndInferenceResults, state ->
        state.copy(
            specimensWithImagesAndInferenceResults = specimensWithImagesAndInferenceResults,
            isLoading = false
        )
    }.onStart {
        loadImagingDetails()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ImagingState(isLoading = true)
    )

    private val _events = Channel<ImagingEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ImagingAction) {
        viewModelScope.launch {
            when (action) {
                ImagingAction.ShowExitDialog -> {
                    _state.update { it.copy(showExitDialog = true) }
                }

                ImagingAction.DismissExitDialog -> {
                    _state.update { it.copy(showExitDialog = false, pendingAction = null) }
                }

                is ImagingAction.SelectPendingAction -> {
                    _state.update { it.copy(pendingAction = action.pendingAction) }
                }

                ImagingAction.ClearPendingAction -> {
                    _state.update { it.copy(pendingAction = null) }
                }

                ImagingAction.ConfirmPendingAction -> {
                    val actionToConfirm = _state.value.pendingAction
                    _state.update { it.copy(showExitDialog = false, pendingAction = null) }
                    actionToConfirm?.let { onAction(it) }
                }

                is ImagingAction.FocusAt -> {
                    _state.update { it.copy(focusPoint = action.offset, isManualFocusing = true) }
                }

                ImagingAction.CancelFocus -> {
                    _state.update { it.copy(focusPoint = null, isManualFocusing = false) }
                }

                is ImagingAction.CorrectSpecimenId -> {
                    _state.update {
                        it.copy(
                            currentSpecimen = it.currentSpecimen.copy(id = action.specimenId),
                            specimenIdError = null
                        )
                    }
                }

                is ImagingAction.ProcessFrame -> {
                    processFrameMutex.withLock {
                        try {
                            if (!_state.value.isCameraReady) {
                                _state.update { it.copy(isCameraReady = true) }
                            }

                            if (!_state.value.isProcessing) {
                                val bitmap = action.frame.toUprightBitmap()

                                val specimenId = inferenceRepository.readSpecimenId(bitmap)
                                validateSpecimenIdUseCase(specimenId, shouldAutoCorrect = true).onSuccess { correctedSpecimenId ->
                                    _state.update {
                                        it.copy(currentSpecimen = it.currentSpecimen.copy(id = correctedSpecimenId))
                                    }
                                }.onError {
                                    _state.update {
                                        it.copy(currentSpecimen = it.currentSpecimen.copy(id = ""))
                                    }
                                }

                                if (_state.value.shouldRunInference) {
                                    val jpegStream = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, jpegStream)
                                    val jpegByteArray = jpegStream.toByteArray()

                                    val bgrMatrix = Imgcodecs.imdecode(MatOfByte(*jpegByteArray), Imgcodecs.IMREAD_COLOR)
                                    val rgbaMatrix = Mat()
                                    Imgproc.cvtColor(bgrMatrix, rgbaMatrix, Imgproc.COLOR_BGR2RGBA)
                                    val jpegBitmap = createBitmap(rgbaMatrix.cols(), rgbaMatrix.rows())
                                    matToBitmap(rgbaMatrix, jpegBitmap)
                                    bgrMatrix.release()
                                    rgbaMatrix.release()

                                    val previewInferenceResults = inferenceRepository.detectSpecimen(jpegBitmap).map { detectorResult ->
                                        InferenceResult(
                                            bboxTopLeftX = detectorResult.bboxTopLeftX,
                                            bboxTopLeftY = detectorResult.bboxTopLeftY,
                                            bboxWidth = detectorResult.bboxWidth,
                                            bboxHeight = detectorResult.bboxHeight,
                                            bboxConfidence = detectorResult.bboxConfidence,
                                            bboxClassId = detectorResult.bboxClassId,
                                            speciesLogits = null,
                                            sexLogits = null,
                                            abdomenStatusLogits = null,
                                            bboxDetectionDuration = detectorResult.bboxDetectionDuration,
                                            speciesInferenceDuration = null,
                                            sexInferenceDuration = null,
                                            abdomenStatusInferenceDuration = null
                                        )
                                    }
                                    val highestConfidenceDetection =
                                        previewInferenceResults.maxByOrNull { it.bboxConfidence }
                                    val autofocusPoint = highestConfidenceDetection?.let { detection ->
                                        inferenceRepository.computeAutofocusCentroid(jpegBitmap, detection)
                                    }

                                    _state.update {
                                        val shouldUseAutofocusThisFrame =
                                            !it.isManualFocusing || (it.focusPoint == null && autofocusPoint != null)

                                        val nextFocusPoint = if (shouldUseAutofocusThisFrame) {
                                            autofocusPoint ?: it.focusPoint
                                        } else {
                                            it.focusPoint
                                        }

                                        val nextIsAutofocusing = when {
                                            !it.isManualFocusing -> true
                                            it.focusPoint == null && autofocusPoint != null -> true
                                            else -> false
                                        }

                                        it.copy(
                                            previewInferenceResults = previewInferenceResults,
                                            focusPoint = nextFocusPoint,
                                            isManualFocusing = !nextIsAutofocusing
                                        )
                                    }

                                    maybeSignalAutoCapture(highestConfidenceDetection)
                                } else {
                                    resetAutoCaptureStability()
                                }
                            }
                        } catch (e: Exception) {
                            emitError(ImagingError.PROCESSING_ERROR)
                        } finally {
                            action.frame.close()
                        }
                    }
                }

                ImagingAction.SaveSessionProgress -> {
                    currentSessionCache.clearSession()
                    _events.send(ImagingEvent.NavigateBackToLandingScreen)
                }

                ImagingAction.SubmitSession -> {
                    val currentSession = currentSessionCache.getSession()
                    val currentSessionSiteId = currentSessionCache.getSiteId()

                    if (currentSession == null || currentSessionSiteId == null) {
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                        return@launch
                    }

                    val success = sessionRepository.markSessionAsComplete(currentSession.localId)
                    if (success) {
                        workRepository.enqueueSessionUpload(
                            currentSession.localId, currentSessionSiteId
                        )
                        currentSessionCache.clearSession()
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                    }
                }

                is ImagingAction.ToggleModelInference -> {
                    val shouldRunInference = action.isChecked
                    resetAutoCaptureStability()
                    _state.update {
                        it.copy(
                            shouldRunInference = shouldRunInference,
                            isAutoCaptureEnabled = if (shouldRunInference) it.isAutoCaptureEnabled else false,
                            previewInferenceResults = emptyList(),
                            currentInferenceResult = null,
                            focusPoint = null
                        )
                    }
                }

                is ImagingAction.ToggleAutoCapture -> {
                    resetAutoCaptureStability()
                    _state.update { it.copy(isAutoCaptureEnabled = action.isChecked) }
                }

                is ImagingAction.CaptureImage -> {
                    if (!_state.value.isCameraReady) return@launch

                    resetAutoCaptureStability()
                    _state.update {
                        it.copy(
                            isProcessing = true,
                            autoCaptureSignal = 0L,
                        )
                    }

                    val captureResult = cameraRepository.captureImage(action.imageCapture)

                    withContext(Dispatchers.Default) {
                        captureResult.onSuccess { image ->
                            val bitmap = image.toUprightBitmap()
                            image.close()

                            val capturedMetadata = action.cameraMetadata?.copy(
                                imageWidth = bitmap.width,
                                imageHeight = bitmap.height,
                                focalPointX = _state.value.focusPoint?.x,
                                focalPointY = _state.value.focusPoint?.y
                            )

                            _state.update { it.copy(currentCameraMetadata = capturedMetadata) }

                            val jpegStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, jpegStream)
                            val jpegByteArray = jpegStream.toByteArray()

                            val bgrMatrix = Imgcodecs.imdecode(MatOfByte(*jpegByteArray), Imgcodecs.IMREAD_COLOR)
                            val rgbaMatrix = Mat()
                            Imgproc.cvtColor(bgrMatrix, rgbaMatrix, Imgproc.COLOR_BGR2RGBA)
                            val jpegBitmap = createBitmap(rgbaMatrix.cols(), rgbaMatrix.rows())
                            matToBitmap(rgbaMatrix, jpegBitmap)
                            bgrMatrix.release()
                            rgbaMatrix.release()

                            if (_state.value.shouldRunInference) {
                                val captureDetectorResults = inferenceRepository.detectSpecimen(jpegBitmap)

                                when (captureDetectorResults.size) {
                                    0 -> emitError(ImagingError.NO_SPECIMEN_FOUND, SnackbarDuration.Short)
                                    1 -> {
                                        val captureDetectorResult = captureDetectorResults.first()
                                        val topLeftXFloat =
                                            captureDetectorResult.bboxTopLeftX * jpegBitmap.width
                                        val topLeftYFloat =
                                            captureDetectorResult.bboxTopLeftY * jpegBitmap.height
                                        val widthFloat = captureDetectorResult.bboxWidth * jpegBitmap.width
                                        val heightFloat = captureDetectorResult.bboxHeight * jpegBitmap.height

                                        val topLeftXAbsolute = topLeftXFloat.toInt()
                                        val topLeftYAbsolute = topLeftYFloat.toInt()
                                        val widthAbsolute =
                                            (widthFloat + (topLeftXFloat - topLeftXAbsolute)).toInt()
                                        val heightAbsolute =
                                            (heightFloat + (topLeftYFloat - topLeftYAbsolute)).toInt()

                                        val clampedTopLeftX =
                                            topLeftXAbsolute.coerceIn(0, jpegBitmap.width - 1)
                                        val clampedTopLeftY =
                                            topLeftYAbsolute.coerceIn(0, jpegBitmap.height - 1)
                                        val clampedWidth =
                                            widthAbsolute.coerceIn(1, jpegBitmap.width - clampedTopLeftX)
                                        val clampedHeight =
                                            heightAbsolute.coerceIn(1, jpegBitmap.height - clampedTopLeftY)

                                        if (clampedWidth > 0 && clampedHeight > 0) {
                                            val croppedBitmap = Bitmap.createBitmap(
                                                jpegBitmap,
                                                clampedTopLeftX,
                                                clampedTopLeftY,
                                                clampedWidth,
                                                clampedHeight
                                            )

                                            var (speciesResult, sexResult, abdomenStatusResult) = inferenceRepository.classifySpecimen(croppedBitmap)

                                            val speciesIndex = speciesResult?.logits?.let { logits -> logits.indexOf(logits.max()) }
                                            var sexIndex = sexResult?.logits?.let { logits -> logits.indexOf(logits.max()) }
                                            var abdomenStatusIndex = abdomenStatusResult?.logits?.let { logits -> logits.indexOf(logits.max()) }

                                            if (speciesResult?.logits == null || speciesIndex == SpeciesLabel.NON_MOSQUITO.ordinal) {
                                                sexResult = null
                                                sexIndex = null
                                            }
                                            if (sexResult?.logits == null || sexIndex == SexLabel.MALE.ordinal) {
                                                abdomenStatusResult = null
                                                abdomenStatusIndex = null
                                            }

                                            _state.update {
                                                it.copy(
                                                    currentSpecimenImage = it.currentSpecimenImage.copy(
                                                        species = speciesIndex?.let { index -> SpeciesLabel.entries[index].label },
                                                        sex = sexIndex?.let { index -> SexLabel.entries[index].label },
                                                        abdomenStatus = abdomenStatusIndex?.let { index -> AbdomenStatusLabel.entries[index].label },
                                                    ),
                                                    currentImageBytes = jpegByteArray,
                                                    currentInferenceResult = InferenceResult(
                                                        bboxTopLeftX = captureDetectorResult.bboxTopLeftX,
                                                        bboxTopLeftY = captureDetectorResult.bboxTopLeftY,
                                                        bboxWidth = captureDetectorResult.bboxWidth,
                                                        bboxHeight = captureDetectorResult.bboxHeight,
                                                        bboxConfidence = captureDetectorResult.bboxConfidence,
                                                        bboxClassId = captureDetectorResult.bboxClassId,
                                                        speciesLogits = speciesResult?.logits,
                                                        sexLogits = sexResult?.logits,
                                                        abdomenStatusLogits = abdomenStatusResult?.logits,
                                                        bboxDetectionDuration = captureDetectorResult.bboxDetectionDuration,
                                                        speciesInferenceDuration = speciesResult?.inferenceDuration,
                                                        sexInferenceDuration = sexResult?.inferenceDuration,
                                                        abdomenStatusInferenceDuration = abdomenStatusResult?.inferenceDuration,
                                                    ),
                                                    previewInferenceResults = emptyList()
                                                )
                                            }
                                        }
                                    }
                                    else -> emitError(ImagingError.MULTIPLE_SPECIMENS_FOUND, SnackbarDuration.Short)
                                }
                            } else {
                                _state.update {
                                    it.copy(
                                        currentImageBytes = jpegByteArray,
                                    )
                                }
                            }
                        }.onError { error ->
                            withContext(Dispatchers.Main) {
                                if (error == ImagingError.NO_ACTIVE_SESSION) {
                                    _events.send(ImagingEvent.NavigateBackToLandingScreen)
                                }
                                emitError(error)
                            }
                        }
                    }
                    _state.update { it.copy(isProcessing = false) }
                }

                ImagingAction.RetakeImage -> {
                    clearStateFields()
                }

                is ImagingAction.TogglePackagingConfirmation -> {
                    val hasConfirmedPackaging = action.isChecked
                    _state.update {
                        it.copy(
                            hasConfirmedPackaging = hasConfirmedPackaging
                        )
                    }
                }

                ImagingAction.SaveImageToSession -> {
                    val currentSession = currentSessionCache.getSession()
                    if (currentSession == null) {
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                        return@launch
                    }

                    val specimenId = when (val validationResult = validateSpecimenIdUseCase(
                        _state.value.currentSpecimen.id, shouldAutoCorrect = false
                    )) {
                        is Result.Success -> {
                            _state.update { it.copy(specimenIdError = null) }
                            validationResult.data
                        }

                        is Result.Error -> {
                            _state.update { it.copy(specimenIdError = validationResult.error) }
                            emitError(validationResult.error)
                            return@launch
                        }
                    }

                    val jpegBytes = _state.value.currentImageBytes ?: return@launch
                    val timestamp = System.currentTimeMillis()
                    val filename = buildString {
                        append(specimenId)
                        append("_")
                        append(timestamp)
                        append(".jpg")
                    }

                    val existingSpecimen = specimenRepository.getSpecimenByIdAndSessionId(
                        specimenId, currentSession.localId
                    )
                    val shouldProcessFurther = when {
                        existingSpecimen != null -> existingSpecimen.shouldProcessFurther
                        _state.value.currentSpecimen.shouldProcessFurther -> true
                        else -> determineSelectionForFurtherProcessing(imagingWorkflow.specimenFurtherProcessingProbability)
                    }

                    if (shouldProcessFurther && !_state.value.hasConfirmedPackaging) {
                        _state.update {
                            it.copy(
                                currentSpecimen = it.currentSpecimen.copy(
                                    shouldProcessFurther = true
                                )
                            )
                        }
                        return@launch
                    }

                    val saveResult = cameraRepository.saveImage(jpegBytes, filename, currentSession)

                    saveResult.onSuccess { imageUri ->
                        val specimen = Specimen(
                            id = specimenId,
                            remoteId = null,
                            shouldProcessFurther = shouldProcessFurther
                        )
                    val specimenImage = SpecimenImage(
                            localId = calculateMd5(jpegBytes),
                            remoteId = null,
                            species = _state.value.currentSpecimenImage.species,
                            sex = _state.value.currentSpecimenImage.sex,
                            abdomenStatus = _state.value.currentSpecimenImage.abdomenStatus,
                            imageUri = imageUri,
                            imageUploadStatus = UploadStatus.NOT_STARTED,
                            metadataUploadStatus = UploadStatus.NOT_STARTED,
                            capturedAt = timestamp,
                            submittedAt = null,
                            imageMetadata = _state.value.currentCameraMetadata
                        )

                        val success = transactionHelper.runAsTransaction {
                            val inferenceResult = _state.value.currentInferenceResult

                            val specimenInsertionResult = if (existingSpecimen == null) {
                                specimenRepository.insertSpecimen(specimen, currentSession.localId)
                            } else {
                                Result.Success(Unit)
                            }
                            val specimenImageInsertionResult =
                                specimenImageRepository.insertSpecimenImage(
                                    specimenImage, specimen.id, currentSession.localId
                                )

                            val inferenceResultInsertionResult = inferenceResult?.let {
                                inferenceResultRepository.insertInferenceResult(
                                    inferenceResult, specimenImage.localId
                                )
                            } ?: Result.Success(Unit)

                            specimenInsertionResult.onError { error ->
                                emitError(error)
                            }

                            specimenImageInsertionResult.onError { error ->
                                emitError(error)
                            }

                            inferenceResultInsertionResult.onError { error ->
                                emitError(error)
                            }

                            (specimenInsertionResult !is Result.Error) && (specimenImageInsertionResult !is Result.Error) && (inferenceResultInsertionResult !is Result.Error)
                        }

                        if (success) {
                            clearStateFields()
                        } else {
                            emitError(ImagingError.SAVE_ERROR)
                            cameraRepository.deleteSavedImage(imageUri)
                        }
                    }.onError { error ->
                        emitError(error)
                    }
                }
            }
        }
    }

    private fun clearStateFields() {
        resetAutoCaptureStability()
        _state.update {
            it.copy(
                autoCaptureSignal = 0L,
                currentSpecimen = it.currentSpecimen.copy(
                    id = "", remoteId = null, shouldProcessFurther = false
                ),
                currentSpecimenImage = it.currentSpecimenImage.copy(
                    localId = "",
                    remoteId = null,
                    species = null,
                    sex = null,
                    abdomenStatus = null,
                    imageUri = Uri.EMPTY,
                    metadataUploadStatus = UploadStatus.NOT_STARTED,
                    imageUploadStatus = UploadStatus.NOT_STARTED,
                    capturedAt = 0L,
                    submittedAt = null
                ),
                currentInferenceResult = null,
                currentImageBytes = null,
                isCameraReady = false,
                previewInferenceResults = emptyList(),
                focusPoint = null,
                isManualFocusing = false,
                hasConfirmedPackaging = false,
                specimenIdError = null,
                currentCameraMetadata = null
            )
        }
    }

    private suspend fun determineSelectionForFurtherProcessing(selectionProbability: Float): Boolean {
        if (selectionProbability == 0f) return false

        val now = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault())

        val startOfCurrentMonth =
            now.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneId.systemDefault())
        val startOfNextMonth = startOfCurrentMonth.plusMonths(1)

        val startDate = startOfCurrentMonth.toInstant().toEpochMilli()
        val endDate = startOfNextMonth.toInstant().toEpochMilli()

        val specimensSelectedForFurtherProcessingThisMonth =
            specimenRepository.countSelectedForFurtherProcessingBetweenSessionCollectionDates(
                startDate,
                endDate
            )

        if (specimensSelectedForFurtherProcessingThisMonth >= MONTHLY_FURTHER_PROCESSING_CAP) return false
        return Random.nextFloat() < selectionProbability
    }

    private fun calculateMd5(imageByteArray: ByteArray): String {
        val md5 = MessageDigest.getInstance("MD5")
        val digest = md5.digest(imageByteArray)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun loadImagingDetails() {
        viewModelScope.launch {
            val session = currentSessionCache.getSession()
            if (session != null) {
                imagingWorkflow = imagingWorkflowFactory.create(session.type)
                val allowModelInferenceToggle = imagingWorkflow.allowModelInferenceToggle
                _state.update {
                    it.copy(
                        allowModelInferenceToggle = allowModelInferenceToggle,
                        shouldRunInference = !allowModelInferenceToggle,
                        sessionType = session.type
                    )
                }
            } else {
                _events.send(ImagingEvent.NavigateBackToLandingScreen)
                emitError(ImagingError.NO_ACTIVE_SESSION)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        inferenceRepository.closeResources()
    }

    private companion object {
        private const val MONTHLY_FURTHER_PROCESSING_CAP = 20
        private const val AUTO_CAPTURE_MIN_CONFIDENCE = 0.5f
        private const val AUTO_CAPTURE_MIN_BBOX_IOU = 0.7f
        /** Max normalized (0–1) L∞ delta between bbox centers for consecutive frames to count as stable. */
        private const val AUTO_CAPTURE_MAX_CENTER_SHIFT = 0.05f
        /** Need at least this many consecutive stable frames (pairs with IoU/center checks) before time gate applies. */
        private const val AUTO_CAPTURE_MIN_STABLE_FRAMES = 2
        private const val AUTO_CAPTURE_MIN_STABLE_DURATION_MS = 2_000L
    }

    private fun resetAutoCaptureStability() {
        lastAutoCaptureBbox = null
        autoCaptureStableFrameCount = 0
        autoCaptureStableStreakStartElapsedMs = 0L
    }

    private fun bboxIoU(a: InferenceResult, b: InferenceResult): Float {
        val ax2 = a.bboxTopLeftX + a.bboxWidth
        val ay2 = a.bboxTopLeftY + a.bboxHeight
        val bx2 = b.bboxTopLeftX + b.bboxWidth
        val by2 = b.bboxTopLeftY + b.bboxHeight
        val interLeft = max(a.bboxTopLeftX, b.bboxTopLeftX)
        val interTop = max(a.bboxTopLeftY, b.bboxTopLeftY)
        val interRight = min(ax2, bx2)
        val interBottom = min(ay2, by2)
        val interW = max(0f, interRight - interLeft)
        val interH = max(0f, interBottom - interTop)
        val interArea = interW * interH
        val areaA = a.bboxWidth * a.bboxHeight
        val areaB = b.bboxWidth * b.bboxHeight
        val union = areaA + areaB - interArea
        return if (union <= 0f) 0f else interArea / union
    }

    /** Largest absolute normalized shift of bbox center along x or y between two detections. */
    private fun bboxMaxCenterShiftNorm(a: InferenceResult, b: InferenceResult): Float {
        val acx = a.bboxTopLeftX + a.bboxWidth * 0.5f
        val acy = a.bboxTopLeftY + a.bboxHeight * 0.5f
        val bcx = b.bboxTopLeftX + b.bboxWidth * 0.5f
        val bcy = b.bboxTopLeftY + b.bboxHeight * 0.5f
        return max(abs(acx - bcx), abs(acy - bcy))
    }

    private fun maybeSignalAutoCapture(highestConfidenceDetection: InferenceResult?) {
        val snapshot = _state.value
        if (!snapshot.isAutoCaptureEnabled || !snapshot.shouldRunInference || snapshot.currentImageBytes != null) {
            resetAutoCaptureStability()
            return
        }

        val det = highestConfidenceDetection
        if (det == null || det.bboxConfidence <= AUTO_CAPTURE_MIN_CONFIDENCE) {
            resetAutoCaptureStability()
            return
        }

        val prev = lastAutoCaptureBbox
        val iouOk = prev != null && bboxIoU(prev, det) >= AUTO_CAPTURE_MIN_BBOX_IOU
        val positionStable =
            prev != null && bboxMaxCenterShiftNorm(prev, det) <= AUTO_CAPTURE_MAX_CENTER_SHIFT
        if (iouOk && positionStable) {
            autoCaptureStableFrameCount++
        } else {
            autoCaptureStableFrameCount = 1
            autoCaptureStableStreakStartElapsedMs = SystemClock.elapsedRealtime()
        }
        lastAutoCaptureBbox = det

        val streakAgeMs =
            if (autoCaptureStableStreakStartElapsedMs > 0L) {
                SystemClock.elapsedRealtime() - autoCaptureStableStreakStartElapsedMs
            } else {
                0L
            }
        if (autoCaptureStableFrameCount >= AUTO_CAPTURE_MIN_STABLE_FRAMES &&
            streakAgeMs >= AUTO_CAPTURE_MIN_STABLE_DURATION_MS
        ) {
            resetAutoCaptureStability()
            _state.update { it.copy(autoCaptureSignal = it.autoCaptureSignal + 1L) }
        }
    }
}
