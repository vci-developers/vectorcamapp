package com.vci.vectorcamapp.imaging.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.util.imaging.ImagingError
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.imaging.domain.repository.InferenceRepository
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ImagingViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val inferenceRepository: InferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImagingState())
    val state: StateFlow<ImagingState> = _state.asStateFlow()

    private val _events = Channel<ImagingEvent>()
    val events = _events.receiveAsFlow()

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
                        _events.send(ImagingEvent.DisplayImagingError(error))
                    }
                }

                ImagingAction.RetakeImage -> {
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

                ImagingAction.SaveImageToSession -> {
                    _state.value.currentImage?.let { bitmap ->
                        val specimenId = _state.value.currentSpecimenId
                        val timestamp = System.currentTimeMillis()
                        val filename = buildString {
                            append(specimenId)
                            append("_")
                            append(timestamp)
                            append(".jpg")
                        }

                        val saveResult = cameraRepository.saveImage(bitmap, filename)

                        saveResult.onError { error ->
                            _events.send(ImagingEvent.DisplayImagingError(error))
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        inferenceRepository.closeResources()
    }
}
