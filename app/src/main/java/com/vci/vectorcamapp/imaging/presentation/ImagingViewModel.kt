package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.repository.InferenceResultRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
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
import kotlinx.coroutines.withContext
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
import androidx.core.graphics.createBitmap
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Mat

@HiltViewModel
class ImagingViewModel @Inject constructor(
    private val currentSessionCache: CurrentSessionCache,
    private val sessionRepository: SessionRepository,
    private val formRepository: FormRepository,
    private val specimenRepository: SpecimenRepository,
    private val specimenImageRepository: SpecimenImageRepository,
    private val inferenceResultRepository: InferenceResultRepository,
    private val cameraRepository: CameraRepository,
    private val inferenceRepository: InferenceRepository,
    private val workRepository: WorkManagerRepository,
    private val validateSpecimenIdUseCase: ValidateSpecimenIdUseCase,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

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
                            }
                        }
                    } catch (e: Exception) {
                        emitError(ImagingError.PROCESSING_ERROR)
                    } finally {
                        action.frame.close()
                    }
                }

                ImagingAction.SaveSessionProgress -> {
                    currentSessionCache.clearSession()
                    _events.send(ImagingEvent.NavigateBackToLandingScreen)
                }

                ImagingAction.SubmitSession -> {
                    val currentSession = currentSessionCache.getSession()
                    if (currentSession == null) {
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                        return@launch
                    }

                    val sessionAndSite = sessionRepository.getSessionAndSiteById(currentSession.localId)
                    val formEntries = buildSessionInputSummaryEntries(currentSession, sessionAndSite?.site) +
                        buildFormSummaryEntries(currentSession.localId)
                    val speciesEntries = buildDetectionSummaryEntries()

                    _state.update {
                        it.copy(
                            showSubmitSummaryDialog = true,
                            submitSummaryFormEntries = formEntries,
                            submitSummarySpeciesEntries = speciesEntries
                        )
                    }
                }

                ImagingAction.DismissSubmitSessionSummaryDialog -> {
                    _state.update {
                        it.copy(
                            showSubmitSummaryDialog = false,
                            submitSummaryFormEntries = emptyList(),
                            submitSummarySpeciesEntries = emptyList()
                        )
                    }
                }

                ImagingAction.ConfirmSubmitSession -> {
                    val currentSession = currentSessionCache.getSession()
                    val currentSessionSiteId = currentSessionCache.getSiteId()
                    if (currentSession == null || currentSessionSiteId == null) {
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                        return@launch
                    }

                    val success = sessionRepository.markSessionAsComplete(currentSession.localId)
                    if (success) {
                        _state.update {
                            it.copy(
                                showSubmitSummaryDialog = false,
                                submitSummaryFormEntries = emptyList(),
                                submitSummarySpeciesEntries = emptyList()
                            )
                        }
                        workRepository.enqueueSessionUpload(
                            currentSession.localId, currentSessionSiteId
                        )
                        currentSessionCache.clearSession()
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                    }
                }

                is ImagingAction.ToggleModelInference -> {
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

                    _state.update { it.copy(isProcessing = true) }

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
    }

    private fun buildSessionInputSummaryEntries(session: Session, site: Site?): List<Pair<String, String>> {
        val dateFormatter = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())

        val entries = mutableListOf<Pair<String, String>>()

        entries += "Session Type" to session.type.name.lowercase()
            .replaceFirstChar { it.uppercase() }

        if (session.collectorName.isNotBlank()) entries += "Collector Name" to session.collectorName
        if (session.collectorTitle.isNotBlank()) entries += "Collector Title" to session.collectorTitle
        if (session.collectorLastTrainedOn > 0L) {
            entries += "Last Trained On" to dateFormatter.format(java.util.Date(session.collectorLastTrainedOn))
        }
        if (session.collectionDate > 0L) {
            entries += "Collection Date" to dateFormatter.format(java.util.Date(session.collectionDate))
        }
        if (session.collectionMethod.isNotBlank()) entries += "Collection Method" to session.collectionMethod
        if (session.specimenCondition.isNotBlank()) entries += "Specimen Condition" to session.specimenCondition

        site?.let { s ->
            s.locationHierarchy?.forEach { (level, value) ->
                if (value.isNotBlank()) entries += level to value
            }
            if (s.district?.isNotBlank() == true && s.locationHierarchy?.containsKey("District") != true) {
                entries += "District" to s.district
            }
            if (s.subCounty?.isNotBlank() == true) entries += "Sub-County" to s.subCounty
            if (s.parish?.isNotBlank() == true) entries += "Parish" to s.parish
            if (s.villageName?.isNotBlank() == true) entries += "Village" to s.villageName
            if (s.houseNumber?.isNotBlank() == true) entries += "House Number" to s.houseNumber
            if (s.healthCenter?.isNotBlank() == true) entries += "Health Center" to s.healthCenter
            if (s.name?.isNotBlank() == true) entries += "Site" to s.name
        }

        if (session.latitude != null && session.longitude != null) {
            entries += "GPS" to "%.6f, %.6f".format(session.latitude, session.longitude)
        }
        if (session.notes.isNotBlank()) entries += "Notes" to session.notes

        return entries
    }

    private suspend fun buildFormSummaryEntries(sessionId: UUID): List<Pair<String, String>> {
        val formsWithAnswers = formRepository.getFormsWithFormAnswersAndQuestionsBySessionId(sessionId)
        return formsWithAnswers
            .flatMap { it.formAnswersAndQuestions }
            .sortedBy { it.question.order ?: Int.MAX_VALUE }
            .map { formAnswerAndQuestion ->
                formAnswerAndQuestion.question.label to formAnswerAndQuestion.answer.value
            }
    }

    private fun buildDetectionSummaryEntries(): List<String> {
        return _state.value.specimensWithImagesAndInferenceResults
            .flatMap { it.specimenImagesAndInferenceResults }
            .map { (specimenImage, _) ->
                buildString {
                    append("Species: ")
                    append(specimenImage.species ?: "N/A")
                    append(", Sex: ")
                    append(specimenImage.sex ?: "N/A")
                    append(", Abdomen Status: ")
                    append(specimenImage.abdomenStatus ?: "N/A")
                }
            }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { (_, count) -> count }
            .map { (summary, count) -> "$summary ($count)" }
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
    }
}
