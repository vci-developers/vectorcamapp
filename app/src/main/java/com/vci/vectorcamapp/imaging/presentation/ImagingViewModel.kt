package com.vci.vectorcamapp.imaging.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.repository.BoundingBoxRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.repository.InferenceRepository
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import com.vci.vectorcamapp.imaging.presentation.model.composites.SpecimenAndBoundingBoxUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImagingViewModel @Inject constructor(
    private val currentSessionCache: CurrentSessionCache,
    private val sessionRepository: SessionRepository,
    private val specimenRepository: SpecimenRepository,
    private val boundingBoxRepository: BoundingBoxRepository,
    private val cameraRepository: CameraRepository,
    private val inferenceRepository: InferenceRepository,
) : ViewModel() {

    @Inject
    lateinit var transactionHelper: TransactionHelper

    private val _capturedSpecimensAndBoundingBoxesUi =
        MutableStateFlow<List<SpecimenAndBoundingBoxUi>>(emptyList())
    private val _state = MutableStateFlow(ImagingState())
    val state = combine(
        _capturedSpecimensAndBoundingBoxesUi, _state
    ) { capturedSpecimensAndBoundingBoxesUi, state ->
        state.copy(
            capturedSpecimensAndBoundingBoxesUi = capturedSpecimensAndBoundingBoxesUi
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ImagingState())

    private val _events = Channel<ImagingEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val currentSession = currentSessionCache.getSession()
            if (currentSession == null) {
                _events.send(ImagingEvent.NavigateBackToLandingScreen)
                _events.send(ImagingEvent.DisplayImagingError(ImagingError.NO_ACTIVE_SESSION))
                return@launch
            }

            sessionRepository.observeSessionWithSpecimens(currentSession.id).filterNotNull()
                .collectLatest { sessionWithSpecimens ->
                    val specimensAndBoundingBoxUiFlows =
                        sessionWithSpecimens.specimens.map { specimen ->
                            specimenRepository.observeSpecimenAndBoundingBox(specimen.id)
                                .filterNotNull().map { specimenAndBoundingBox ->
                                    val boundingBoxUi = inferenceRepository.convertToBoundingBoxUi(
                                        specimenAndBoundingBox.boundingBox
                                    )
                                    SpecimenAndBoundingBoxUi(
                                        specimen = specimenAndBoundingBox.specimen,
                                        boundingBoxUi = boundingBoxUi
                                    )
                                }
                        }
                    if (specimensAndBoundingBoxUiFlows.isEmpty()) {
                        _capturedSpecimensAndBoundingBoxesUi.value = emptyList()
                    } else {
                        combine(specimensAndBoundingBoxUiFlows) { it.toList() }.collect { capturedSpecimensAndBoundingBoxesUi ->
                            _capturedSpecimensAndBoundingBoxesUi.value =
                                capturedSpecimensAndBoundingBoxesUi
                        }
                    }
                }
        }
    }

    fun onAction(action: ImagingAction) {
        viewModelScope.launch {
            when (action) {
                is ImagingAction.CorrectSpecimenId -> {
                    _state.update {
                        it.copy(
                            currentSpecimen = it.currentSpecimen.copy(
                                id = action.specimenId
                            )
                        )
                    }
                }

                is ImagingAction.ProcessFrame -> {
                    try {
                        val bitmap = action.frame.toUprightBitmap()

                        val specimenId = inferenceRepository.readSpecimenId(bitmap)
                        val (_, boundingBox) = inferenceRepository.detectSpecimen(bitmap)

                        _state.update {
                            it.copy(
                                currentSpecimen = it.currentSpecimen.copy(id = specimenId),
                                currentBoundingBoxUi = boundingBox?.let { boundingBox ->
                                    inferenceRepository.convertToBoundingBoxUi(boundingBox)
                                })
                        }
                    } catch (e: Exception) {
                        Log.e("ViewModel", "Image processing setup failed: ${e.message}")
                    } finally {
                        action.frame.close()
                    }
                }

                is ImagingAction.CaptureImage -> {
                    _state.update { it.copy(isCapturing = true) }

                    val captureResult = cameraRepository.captureImage(action.controller)

                    _state.update { it.copy(isCapturing = false) }

                    captureResult.onSuccess { image ->
                        val bitmap = image.toUprightBitmap()
                        image.close()

                        val (species, sex, abdomenStatus) = inferenceRepository.classifySpecimen(
                            bitmap
                        )

                        if (species != null) {
                            _state.update {
                                it.copy(
                                    currentSpecimen = it.currentSpecimen.copy(
                                        species = species.label,
                                        sex = sex?.label,
                                        abdomenStatus = abdomenStatus?.label,
                                    ), currentImage = bitmap
                                )
                            }
                        } else {
                            _events.send(ImagingEvent.DisplayImagingError(ImagingError.NO_SPECIMEN_FOUND))
                        }
                    }.onError { error ->
                        if (error == ImagingError.NO_ACTIVE_SESSION) {
                            _events.send(ImagingEvent.NavigateBackToLandingScreen)
                        }
                        _events.send(ImagingEvent.DisplayImagingError(error))
                    }
                }

                ImagingAction.RetakeImage -> {
                    clearCurrentSpecimenStateFields()
                }

                ImagingAction.SaveImageToSession -> {
                    val bitmap = _state.value.currentImage ?: return@launch
                    val specimenId = _state.value.currentSpecimen.id
                    val timestamp = System.currentTimeMillis()
                    val filename = buildString {
                        append(specimenId)
                        append("_")
                        append(timestamp)
                        append(".jpg")
                    }

                    val currentSession = currentSessionCache.getSession()
                    if (currentSession == null) {
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                        return@launch
                    }

                    val saveResult = cameraRepository.saveImage(bitmap, filename, currentSession)

                    saveResult.onSuccess { imageUri ->
                        val specimen = Specimen(
                            id = specimenId,
                            species = _state.value.currentSpecimen.species,
                            sex = _state.value.currentSpecimen.sex,
                            abdomenStatus = _state.value.currentSpecimen.abdomenStatus,
                            imageUri = imageUri,
                            capturedAt = timestamp
                        )

                        val success = transactionHelper.runAsTransaction {
                            val boundingBoxUi =
                                _state.value.currentBoundingBoxUi ?: return@runAsTransaction false
                            val boundingBox = inferenceRepository.convertToBoundingBox(
                                boundingBoxUi
                            )

                            val specimenResult =
                                specimenRepository.insertSpecimen(specimen, currentSession.id)
                            val boundingBoxResult =
                                boundingBoxRepository.insertBoundingBox(boundingBox, specimen.id)

                            specimenResult.onError { error ->
                                Log.d("ROOM ERROR", "Specimen error: $error")
                                return@runAsTransaction false
                            }

                            boundingBoxResult.onError { error ->
                                Log.d("ROOM ERROR", "Bounding box error: $error")
                                return@runAsTransaction false
                            }

                            true
                        }

                        if (success) {
                            clearCurrentSpecimenStateFields()
                        }
                    }.onError { error ->
                        _events.send(ImagingEvent.DisplayImagingError(error))
                    }
                }

                ImagingAction.SubmitSession -> {
                    val currentSession = currentSessionCache.getSession()
                    if (currentSession == null) {
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                        return@launch
                    }
                    val success = sessionRepository.markSessionAsComplete(currentSession.id)
                    if (success) {
                        currentSessionCache.clearSession()
                        _events.send(ImagingEvent.NavigateBackToLandingScreen)
                        // TODO: Trigger background task to upload images to cloud
                    }
                }
            }
        }
    }

    private fun clearCurrentSpecimenStateFields() {
        _state.update {
            it.copy(
                currentSpecimen = it.currentSpecimen.copy(
                    id = "", species = null, sex = null, abdomenStatus = null
                ),
                currentImage = null,
                currentBoundingBoxUi = null,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()

        inferenceRepository.closeResources()
    }
}
