package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.imaging.data.TfLiteSpecimenClassifier
import com.vci.vectorcamapp.imaging.di.AbdomenStatusClassifier
import com.vci.vectorcamapp.imaging.di.SexClassifier
import com.vci.vectorcamapp.imaging.di.SpeciesClassifier
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
import com.vci.vectorcamapp.imaging.presentation.extensions.resizeTo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class ImagingViewModel @Inject constructor(
    @SpeciesClassifier private val speciesClassifier: SpecimenClassifier,
    @SexClassifier private val sexClassifier: SpecimenClassifier,
    @AbdomenStatusClassifier private val abdomenStatusClassifier: SpecimenClassifier,
) : ViewModel() {

    private val _state = MutableStateFlow(ImagingState())
    val state: StateFlow<ImagingState> = _state.asStateFlow()

    private val _events = Channel<ImagingEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ImagingAction) {
        viewModelScope.launch {
            when (action) {
                is ImagingAction.UpdateSpecimenId -> {
                    _state.update {
                        it.copy(currentSpecimenId = action.specimenId)
                    }
                }

                is ImagingAction.UpdateBoundingBoxUi -> {
                    _state.update {
                        it.copy(currentBoundingBoxUi = action.boundingBoxUi)
                    }
                }

                is ImagingAction.CaptureComplete -> {
                    action.result.onSuccess { bitmap ->
                        withContext(Dispatchers.Default) {

                            val speciesPromise = async { getClassification(bitmap, speciesClassifier) }
                            val sexPromise = async { getClassification(bitmap, sexClassifier) }
                            val abdomenStatusPromise = async { getClassification(bitmap, abdomenStatusClassifier) }

                            val species = speciesPromise.await()
                            val sex = sexPromise.await()
                            val abdomenStatus = abdomenStatusPromise.await()

                            withContext(Dispatchers.Main) {
                                _state.update {
                                    it.copy(
                                        currentImage = bitmap,
                                    )
                                }
                            }
                        }
                    }.onError { error ->
                        _events.send(ImagingEvent.DisplayImagingError(error))
                    }
                }

                ImagingAction.RetakeImage -> {
                    _state.update {
                        it.copy(
                            currentSpecimenId = "",
                            currentImage = null,
                            currentBoundingBoxUi = null,
                        )
                    }
                }
            }
        }
    }

    private suspend fun getClassification(bitmap: Bitmap, classifier: SpecimenClassifier): Int? {
        val (classifierTensorWidth, classifierTensorHeight) = classifier.getInputTensorShape()

        return classifier.classify(
            bitmap.resizeTo(
                classifierTensorWidth,
                classifierTensorHeight
            )
        )
    }
}
