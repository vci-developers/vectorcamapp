package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material3.SnackbarDuration
import androidx.core.graphics.createBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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
import com.vci.vectorcamapp.core.logging.analytics.VectorCamAnalytics
import com.vci.vectorcamapp.core.logging.crashlytics.Severity
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlytics
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlyticsContext
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
import com.vci.vectorcamapp.imaging.domain.util.ImagingError
import com.vci.vectorcamapp.imaging.presentation.enums.CaptureStage
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import com.vci.vectorcamapp.navigation.Destination
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
import kotlinx.coroutines.withContext
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class ImagingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
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
    private val destination = savedStateHandle.toRoute<Destination.Imaging>()
    private val sessionUnitId: UUID? = destination.sessionUnitId?.let(UUID::fromString)

    @Inject
    lateinit var transactionHelper: TransactionHelper

    @Inject
    lateinit var imagingWorkflowFactory: ImagingWorkflowFactory
    private lateinit var imagingWorkflow: ImagingWorkflow

    // ── Analytics timing fields ───────────────────────────────────────────────
    private var sessionStartedAt: Long = 0L
    private var captureStartedAt: Long = 0L
    private var inferenceStartedAt: Long = 0L
    private var firstFrameLogged: Boolean = false
    private var lastFocusLogMs: Long = 0L
    private var lastFrameErrorMs: Long = 0L

    private val _specimensWithImagesAndInferenceResults: Flow<List<SpecimenWithSpecimenImagesAndInferenceResults>> =
        flow {
            val session = currentSessionCache.getSession()
            if (session == null) {
                emit(emptyList())
            } else {
                specimenRepository.observeSpecimenImagesAndInferenceResultsBySessionScope(
                    sessionId = session.localId,
                    sessionUnitId = sessionUnitId
                ).collect { emit(it) }
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
                    VectorCamAnalytics.logEvent("imaging_exit_dialog_opened")
                    _state.update { it.copy(showExitDialog = true) }
                }

                ImagingAction.DismissExitDialog -> {
                    VectorCamAnalytics.logEvent("imaging_exit_dialog_dismissed")
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
                    VectorCamAnalytics.logEvent(
                        "imaging_pending_action_confirmed",
                        mapOf("pending_action_class" to (actionToConfirm?.let { it::class.simpleName } ?: "null"))
                    )
                    _state.update { it.copy(showExitDialog = false, pendingAction = null) }
                    actionToConfirm?.let { onAction(it) }
                }

                is ImagingAction.FocusAt -> {
                    val now = System.currentTimeMillis()
                    if (now - lastFocusLogMs >= 2_000) {
                        lastFocusLogMs = now
                        VectorCamAnalytics.logEvent(
                            "imaging_manual_focus_set",
                            mapOf("x_norm" to action.offset.x, "y_norm" to action.offset.y)
                        )
                    }
                    _state.update { it.copy(focusPoint = action.offset, isManualFocusing = true) }
                }

                ImagingAction.CancelFocus -> {
                    VectorCamAnalytics.logEvent("imaging_focus_cancelled")
                    _state.update { it.copy(focusPoint = null, isManualFocusing = false) }
                }

                is ImagingAction.CorrectSpecimenId -> {
                    VectorCamAnalytics.logEvent(
                        "imaging_specimen_id_corrected",
                        mapOf(
                            "length" to action.specimenId.length,
                            "had_previous_error" to (_state.value.specimenIdError != null)
                        )
                    )
                    _state.update {
                        it.copy(
                            currentSpecimen = it.currentSpecimen.copy(id = action.specimenId),
                            specimenIdError = null
                        )
                    }
                }

                is ImagingAction.ProcessFrame -> {
                    try {
                        if (!_state.value.isCameraReady) {
                            _state.update { it.copy(isCameraReady = true) }
                            if (!firstFrameLogged) {
                                firstFrameLogged = true
                                VectorCamAnalytics.logEvent(
                                    "imaging_camera_opened",
                                    mapOf(
                                        "latency_ms_since_session_started" to
                                                (System.currentTimeMillis() - sessionStartedAt)
                                    )
                                )
                            }
                        }

                        if (_state.value.captureStage == null) {
                            val bitmap = action.frame.toUprightBitmap()

                            val specimenId = inferenceRepository.readSpecimenId(bitmap)
                            validateSpecimenIdUseCase(
                                specimenId,
                                shouldAutoCorrect = true
                            ).onSuccess { correctedSpecimenId ->
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

                                val bgrMatrix = Imgcodecs.imdecode(
                                    MatOfByte(*jpegByteArray),
                                    Imgcodecs.IMREAD_COLOR
                                )
                                val rgbaMatrix = Mat()
                                Imgproc.cvtColor(bgrMatrix, rgbaMatrix, Imgproc.COLOR_BGR2RGBA)
                                val jpegBitmap = createBitmap(rgbaMatrix.cols(), rgbaMatrix.rows())
                                matToBitmap(rgbaMatrix, jpegBitmap)
                                bgrMatrix.release()
                                rgbaMatrix.release()

                                val previewInferenceResults =
                                    inferenceRepository.detectSpecimen(jpegBitmap)
                                        .map { detectorResult ->
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
                                    inferenceRepository.computeAutofocusCentroid(
                                        jpegBitmap,
                                        detection
                                    )
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
                            }
                        }
                    } catch (e: Exception) {
                        val now = System.currentTimeMillis()
                        if (now - lastFrameErrorMs >= 5_000) {
                            lastFrameErrorMs = now
                            VectorCamAnalytics.logEvent(
                                "imaging_frame_processing_failed",
                                mapOf("error_class" to ImagingError.PROCESSING_ERROR.name)
                            )
                        }
                        emitError(ImagingError.PROCESSING_ERROR)
                    } finally {
                        action.frame.close()
                    }
                }

                ImagingAction.SaveSessionProgress -> {
                    val sessionId = currentSessionCache.getSession()?.localId?.toString() ?: ""
                    val specimenCount = _state.value.specimensWithImagesAndInferenceResults.size
                    VectorCamAnalytics.logEvent(
                        "imaging_session_progress_saved",
                        mapOf(
                            "session_id" to sessionId,
                            "total_specimens" to specimenCount,
                            "total_duration_ms" to (System.currentTimeMillis() - sessionStartedAt)
                        )
                    )
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
                        val specimenCount = _state.value.specimensWithImagesAndInferenceResults.size
                        VectorCamAnalytics.logEvent(
                            "imaging_session_submitted",
                            mapOf(
                                "session_id" to currentSession.localId.toString(),
                                "session_type" to _state.value.sessionType.name,
                                "total_specimens" to specimenCount,
                                "total_duration_ms" to (System.currentTimeMillis() - sessionStartedAt)
                            )
                        )
                        workRepository.enqueueSessionUpload(
                            currentSession.localId, currentSessionSiteId
                        )
                        currentSessionCache.clearSession()
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                    }
                }

                is ImagingAction.ToggleModelInference -> {
                    VectorCamAnalytics.logEvent(
                        "imaging_inference_toggled",
                        mapOf("is_enabled" to action.isChecked)
                    )
                    val shouldRunInference = action.isChecked
                    _state.update {
                        it.copy(
                            shouldRunInference = shouldRunInference,
                            previewInferenceResults = emptyList(),
                            currentInferenceResult = null,
                            focusPoint = null
                        )
                    }
                }

                is ImagingAction.CaptureImage -> {
                    if (!_state.value.isCameraReady) return@launch

                    captureStartedAt = System.currentTimeMillis()
                    VectorCamAnalytics.logEvent(
                        "imaging_capture_shutter_pressed",
                        mapOf(
                            "session_id" to (currentSessionCache.getSession()?.localId?.toString() ?: ""),
                            "inference_enabled" to _state.value.shouldRunInference,
                            "manual_focus" to _state.value.isManualFocusing,
                            "current_specimen_count" to _state.value.specimensWithImagesAndInferenceResults.size
                        )
                    )

                    _state.update { it.copy(captureStage = CaptureStage.CAPTURING) }

                    val captureResult = cameraRepository.captureImage(action.imageCapture)

                    withContext(Dispatchers.Default) {
                        captureResult.onSuccess { image ->
                            val bitmap = image.toUprightBitmap()
                            image.close()

                            val captureDurationMs = System.currentTimeMillis() - captureStartedAt
                            VectorCamAnalytics.logEvent(
                                "imaging_specimen_image_captured",
                                mapOf(
                                    "capture_duration_ms" to captureDurationMs,
                                    "image_width" to bitmap.width,
                                    "image_height" to bitmap.height,
                                    "had_focus_point" to (_state.value.focusPoint != null)
                                )
                            )

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

                            val bgrMatrix = Imgcodecs.imdecode(
                                MatOfByte(*jpegByteArray),
                                Imgcodecs.IMREAD_COLOR
                            )
                            val rgbaMatrix = Mat()
                            Imgproc.cvtColor(bgrMatrix, rgbaMatrix, Imgproc.COLOR_BGR2RGBA)
                            val jpegBitmap = createBitmap(rgbaMatrix.cols(), rgbaMatrix.rows())
                            matToBitmap(rgbaMatrix, jpegBitmap)
                            bgrMatrix.release()
                            rgbaMatrix.release()

                            if (_state.value.shouldRunInference) {
                                _state.update { it.copy(captureStage = CaptureStage.DETECTING) }

                                val detectionStartMs = System.currentTimeMillis()
                                val captureDetectorResults =
                                    inferenceRepository.detectSpecimen(jpegBitmap)
                                val detectionDurationMs = System.currentTimeMillis() - detectionStartMs

                                val outcome = when (captureDetectorResults.size) {
                                    0 -> "none"
                                    1 -> "found_one"
                                    else -> "multiple"
                                }
                                VectorCamAnalytics.logEvent(
                                    "imaging_specimen_detection_completed",
                                    mapOf(
                                        "detection_count" to captureDetectorResults.size,
                                        "top_confidence" to (captureDetectorResults.maxByOrNull { it.bboxConfidence }?.bboxConfidence),
                                        "duration_ms" to detectionDurationMs,
                                        "outcome" to outcome
                                    )
                                )

                                when (captureDetectorResults.size) {
                                    0 -> emitError(
                                        ImagingError.NO_SPECIMEN_FOUND,
                                        SnackbarDuration.Short
                                    )

                                    1 -> {
                                        val captureDetectorResult = captureDetectorResults.first()
                                        val topLeftXFloat =
                                            captureDetectorResult.bboxTopLeftX * jpegBitmap.width
                                        val topLeftYFloat =
                                            captureDetectorResult.bboxTopLeftY * jpegBitmap.height
                                        val widthFloat =
                                            captureDetectorResult.bboxWidth * jpegBitmap.width
                                        val heightFloat =
                                            captureDetectorResult.bboxHeight * jpegBitmap.height

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
                                            widthAbsolute.coerceIn(
                                                1,
                                                jpegBitmap.width - clampedTopLeftX
                                            )
                                        val clampedHeight =
                                            heightAbsolute.coerceIn(
                                                1,
                                                jpegBitmap.height - clampedTopLeftY
                                            )

                                        if (clampedWidth > 0 && clampedHeight > 0) {
                                            val croppedBitmap = Bitmap.createBitmap(
                                                jpegBitmap,
                                                clampedTopLeftX,
                                                clampedTopLeftY,
                                                clampedWidth,
                                                clampedHeight
                                            )

                                            _state.update { it.copy(captureStage = CaptureStage.CLASSIFYING) }

                                            inferenceStartedAt = System.currentTimeMillis()
                                            var (speciesResult, sexResult, abdomenStatusResult) = inferenceRepository.classifySpecimen(
                                                croppedBitmap
                                            )
                                            val totalInferenceDurationMs = System.currentTimeMillis() - inferenceStartedAt

                                            val speciesIndex =
                                                speciesResult?.logits?.let { logits ->
                                                    logits.indexOf(logits.max())
                                                }
                                            var sexIndex = sexResult?.logits?.let { logits ->
                                                logits.indexOf(logits.max())
                                            }
                                            var abdomenStatusIndex =
                                                abdomenStatusResult?.logits?.let { logits ->
                                                    logits.indexOf(logits.max())
                                                }

                                            if (speciesResult?.logits == null || speciesIndex == SpeciesLabel.NON_MOSQUITO.ordinal) {
                                                sexResult = null
                                                sexIndex = null
                                            }
                                            if (sexResult?.logits == null || sexIndex == SexLabel.MALE.ordinal) {
                                                abdomenStatusResult = null
                                                abdomenStatusIndex = null
                                            }

                                            val speciesLabel = speciesIndex?.let { SpeciesLabel.entries[it].label }
                                            val sexLabel = sexIndex?.let { SexLabel.entries[it].label }
                                            val abdomenLabel = abdomenStatusIndex?.let { AbdomenStatusLabel.entries[it].label }

                                            VectorCamAnalytics.logEvent(
                                                "imaging_specimen_inference_completed",
                                                mapOf(
                                                    "species_label" to speciesLabel,
                                                    "sex_label" to sexLabel,
                                                    "abdomen_status_label" to abdomenLabel,
                                                    "bbox_confidence" to captureDetectorResult.bboxConfidence,
                                                    "species_inference_duration_ms" to speciesResult?.inferenceDuration,
                                                    "sex_inference_duration_ms" to sexResult?.inferenceDuration,
                                                    "abdomen_status_inference_duration_ms" to abdomenStatusResult?.inferenceDuration,
                                                    "total_inference_duration_ms" to totalInferenceDurationMs
                                                )
                                            )

                                            _state.update {
                                                it.copy(
                                                    currentSpecimenImage = it.currentSpecimenImage.copy(
                                                        species = speciesLabel,
                                                        sex = sexLabel,
                                                        abdomenStatus = abdomenLabel,
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

                                    else -> emitError(
                                        ImagingError.MULTIPLE_SPECIMENS_FOUND,
                                        SnackbarDuration.Short
                                    )
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
                                    VectorCamCrashlytics.exception(
                                        throwable = IllegalStateException("No active session during capture"),
                                        severity = Severity.ERROR,
                                        context = VectorCamCrashlyticsContext(screen = "Imaging", feature = "CaptureImage", action = "capture"),
                                    )
                                    VectorCamAnalytics.logEvent(
                                        "imaging_no_active_session",
                                        mapOf("error_class" to ImagingError.NO_ACTIVE_SESSION.name)
                                    )
                                    _events.send(ImagingEvent.NavigateBackToLandingScreen)
                                }
                                emitError(error)
                            }
                        }
                    }
                    _state.update { it.copy(captureStage = null) }
                }

                ImagingAction.RetakeImage -> {
                    VectorCamAnalytics.logEvent(
                        "imaging_image_retake_clicked",
                        mapOf("had_inference_result" to (_state.value.currentInferenceResult != null))
                    )
                    clearStateFields()
                }

                is ImagingAction.TogglePackagingConfirmation -> {
                    VectorCamAnalytics.logEvent(
                        "imaging_packaging_confirmation_toggled",
                        mapOf("is_checked" to action.isChecked)
                    )
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

                    if (existingSpecimen != null) {
                        val existingSessionUnitId = specimenRepository.getSessionUnitIdForSpecimen(
                            specimenId, currentSession.localId
                        )
                        if (existingSessionUnitId != _state.value.sessionUnitId) {
                            _state.update {
                                it.copy(specimenIdError = ImagingError.SPECIMEN_ID_USED_IN_ANOTHER_COLLECTION_BATCH)
                            }
                            emitError(ImagingError.SPECIMEN_ID_USED_IN_ANOTHER_COLLECTION_BATCH)
                            return@launch
                        }
                    }

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
                                specimenRepository.insertSpecimen(
                                    specimen = specimen,
                                    sessionId = currentSession.localId,
                                    sessionUnitId = _state.value.sessionUnitId,
                                )
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
                            val totalSpecimens = _state.value.specimensWithImagesAndInferenceResults.size + 1
                            VectorCamAnalytics.logEvent(
                                "imaging_specimen_image_saved",
                                mapOf(
                                    "session_id" to currentSession.localId.toString(),
                                    "session_unit_id" to (_state.value.sessionUnitId?.toString()),
                                    "specimen_id" to specimenId,
                                    "is_new_specimen" to (existingSpecimen == null),
                                    "should_process_further" to shouldProcessFurther,
                                    "image_size_bytes" to jpegBytes.size.toLong(),
                                    "total_specimens_so_far" to totalSpecimens
                                )
                            )
                            clearStateFields()
                        } else {
                            emitError(ImagingError.SAVE_ERROR)
                            cameraRepository.deleteSavedImage(imageUri)
                        }
                    }.onError { error ->
                        emitError(error)
                    }
                }

                ImagingAction.ReturnToCollectionBatchList -> {
                    VectorCamAnalytics.logEvent("imaging_return_to_collection_batch_clicked")
                    val currentSession = currentSessionCache.getSession()
                    if (currentSession == null) {
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                        return@launch
                    }
                    _events.send(
                        ImagingEvent.NavigateBackToCollectionBatchListScreen(currentSession.localId)
                    )
                }
            }
        }
    }

    private fun clearStateFields() {
        _state.update {
            it.copy(
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
        firstFrameLogged = false
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
                VectorCamAnalytics.updateDeviceCondition()
                imagingWorkflow = imagingWorkflowFactory.create(session.type)
                val allowModelInferenceToggle = imagingWorkflow.allowModelInferenceToggle
                sessionStartedAt = System.currentTimeMillis()
                firstFrameLogged = false
                _state.update {
                    it.copy(
                        allowModelInferenceToggle = allowModelInferenceToggle,
                        shouldRunInference = !allowModelInferenceToggle,
                        sessionType = session.type,
                        sessionUnitId = sessionUnitId,
                    )
                }
                VectorCamAnalytics.logEvent(
                    "imaging_session_started",
                    mapOf(
                        "session_id" to session.localId.toString(),
                        "session_unit_id" to sessionUnitId?.toString(),
                        "session_type" to session.type.name,
                        "allow_inference_toggle" to allowModelInferenceToggle,
                        "inference_enabled" to !allowModelInferenceToggle
                    )
                )
            } else {
                _events.send(ImagingEvent.NavigateBackToLandingScreen)
                emitError(ImagingError.NO_ACTIVE_SESSION)
            }
        }
    }

    private companion object {
        private const val MONTHLY_FURTHER_PROCESSING_CAP = 20
    }
}

