package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.imaging.data.GpuDelegateManager
import com.vci.vectorcamapp.imaging.di.AbdomenStatusClassifier
import com.vci.vectorcamapp.imaging.di.Detector
import com.vci.vectorcamapp.imaging.di.SexClassifier
import com.vci.vectorcamapp.imaging.di.SpeciesClassifier
import com.vci.vectorcamapp.imaging.di.SpecimenIdRecognizer
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
import com.vci.vectorcamapp.imaging.domain.SpecimenDetector
import com.vci.vectorcamapp.imaging.presentation.extensions.cropToBoundingBoxAndPad
import com.vci.vectorcamapp.imaging.presentation.extensions.resizeTo
import com.vci.vectorcamapp.imaging.presentation.extensions.toBoundingBoxUi
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@HiltViewModel
class ImagingViewModel @Inject constructor(
    @SpecimenIdRecognizer private val specimenIdRecognizer: TextRecognizer,
    @Detector private val specimenDetector: SpecimenDetector,
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
                is ImagingAction.ProcessFrame -> {
                    try {
                        val bitmap = action.frame.toUprightBitmap()
                        val inputImage = InputImage.fromBitmap(bitmap, 0)

                        coroutineScope {
                            launch(Dispatchers.IO) {
                                try {
                                    val specimenId = suspendCoroutine { continuation ->
                                        specimenIdRecognizer.process(inputImage)
                                            .addOnSuccessListener { visionText: Text ->
                                                continuation.resume(visionText.text)
                                            }.addOnFailureListener { exception ->
                                                Log.e(
                                                    "ViewModel",
                                                    "Text recognition failed: ${exception.message}"
                                                )
                                                continuation.resume("")
                                            }
                                    }
                                    withContext(Dispatchers.Main) {
                                        _state.update { it.copy(currentSpecimenId = specimenId) }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ViewModel", "Specimen ID analysis failed: ${e.message}")
                                }

                                try {
                                    val (detectorTensorHeight, detectorTensorWidth) = specimenDetector.getInputTensorShape()

                                    val resized =
                                        bitmap.resizeTo(detectorTensorWidth, detectorTensorHeight)
                                    val boundingBox = specimenDetector.detect(resized)

                                    withContext(Dispatchers.Main) {
                                        _state.update {
                                            it.copy(
                                                currentBoundingBoxUi = boundingBox?.toBoundingBoxUi(
                                                    detectorTensorWidth,
                                                    detectorTensorHeight,
                                                    bitmap.width,
                                                    bitmap.height
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        "ViewModel", "Bounding box detection failed: ${e.message}"
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ViewModel", "Image processing setup failed: ${e.message}")
                    } finally {
                        action.frame.close()
                    }
                }

                is ImagingAction.CaptureComplete -> {
                    action.result.onSuccess { image ->
                        val bitmap = image.toUprightBitmap()

                        withContext(Dispatchers.Default) {
                            val (detectorTensorHeight, detectorTensorWidth) = specimenDetector.getInputTensorShape()

                            val resized = bitmap.resizeTo(detectorTensorWidth, detectorTensorHeight)
                            val boundingBox = specimenDetector.detect(resized)

                            val croppedAndPadded = boundingBox?.let {
                                resized.cropToBoundingBoxAndPad(it)
                            }

                            croppedAndPadded?.let {
                                val speciesPromise =
                                    async { getClassification(it, speciesClassifier) }
                                val sexPromise = async { getClassification(it, sexClassifier) }
                                val abdomenStatusPromise =
                                    async { getClassification(it, abdomenStatusClassifier) }

                                val species = speciesPromise.await()
                                val sex = sexPromise.await()
                                val abdomenStatus = abdomenStatusPromise.await()
                            }

                            withContext(Dispatchers.Main) {
                                _state.update {
                                    it.copy(
                                        currentImage = croppedAndPadded,
                                    )
                                }
                                image.close()
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
        val (classifierTensorHeight, classifierTensorWidth) = classifier.getInputTensorShape()

        return classifier.classify(bitmap.resizeTo(classifierTensorWidth, classifierTensorHeight))
    }

    override fun onCleared() {
        super.onCleared()

        specimenIdRecognizer.close()
        specimenDetector.close()
        speciesClassifier.close()
        sexClassifier.close()
        abdomenStatusClassifier.close()
        GpuDelegateManager.close()
    }
}
