package com.vci.vectorcamapp.imaging.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.composites.SessionWithSpecimens
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.repository.InferenceRepository
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class ImagingViewModel @Inject constructor(
    private val currentSessionCache: CurrentSessionCache,
    private val sessionRepository: SessionRepository,
    private val specimenRepository: SpecimenRepository,
    private val cameraRepository: CameraRepository,
    private val inferenceRepository: InferenceRepository,
) : ViewModel() {

    private val _sessionWithSpecimens = MutableStateFlow(
        SessionWithSpecimens(
            session = Session(
                id = UUID(0, 0), createdAt = 0L, submittedAt = null
            ), specimens = emptyList()
        )
    )
    private val _state = MutableStateFlow(ImagingState())
    val state = combine(_sessionWithSpecimens, _state) { sessionWithSpecimens, state ->
        state.copy(
            specimens = sessionWithSpecimens.specimens
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
                .collect { sessionWithSpecimens ->
                    _sessionWithSpecimens.value = sessionWithSpecimens
                }
        }
    }

    fun onAction(action: ImagingAction) {
        viewModelScope.launch {
            when (action) {
                is ImagingAction.CorrectSpecimenId -> {
                    _state.update { it.copy(currentSpecimenId = action.specimenId) }
                }

                is ImagingAction.ProcessFrame -> {
                    try {
                        val bitmap = action.frame.toUprightBitmap()

                        val specimenId = inferenceRepository.readSpecimenId(bitmap)
                        val (_, boundingBox) = inferenceRepository.detectSpecimen(bitmap)
                        val boundingBoxUi = inferenceRepository.convertToBoundingBoxUi(
                            boundingBox, bitmap.width, bitmap.height
                        )

                        _state.update {
                            it.copy(
                                currentSpecimenId = specimenId, currentBoundingBoxUi = boundingBoxUi
                            )
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
                                    currentSpecies = species.label,
                                    currentSex = sex?.label,
                                    currentAbdomenStatus = abdomenStatus?.label,
                                    currentImage = bitmap
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
                    clearSpecimenStateFields()
                }

                ImagingAction.SaveImageToSession -> {
                    val bitmap = _state.value.currentImage ?: return@launch
                    val specimenId = _state.value.currentSpecimenId
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
                            species = _state.value.currentSpecies,
                            sex = _state.value.currentSex,
                            abdomenStatus = _state.value.currentAbdomenStatus,
                            imageUri = imageUri,
                            capturedAt = timestamp
                        )

                        if (specimenRepository.upsertSpecimen(specimen, currentSession.id)) {
                            clearSpecimenStateFields()
                        }
                    }.onError { error ->
                        _events.send(ImagingEvent.DisplayImagingError(error))
                    }
                }
            }
        }
    }

    private fun clearSpecimenStateFields() {
        _state.update {
            it.copy(
                currentSpecimenId = "",
                currentSpecies = null,
                currentSex = null,
                currentAbdomenStatus = null,
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
