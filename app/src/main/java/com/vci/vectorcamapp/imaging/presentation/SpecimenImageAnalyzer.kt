package com.vci.vectorcamapp.imaging.presentation

import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vci.vectorcamapp.imaging.domain.Detection
import com.vci.vectorcamapp.imaging.domain.SpecimenDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SpecimenImageAnalyzer(
    private val detector: SpecimenDetector,
    private val onDetectionUpdated: (Detection?) -> Unit,
    private val onSpecimenIdUpdated: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val specimenIdRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class) // TODO - Remove when possible
    override fun analyze(image: ImageProxy) {
        scope.launch {
            val mediaImage: Image = image.image ?: run { image.close(); return@launch }
            val inputImage: InputImage =
                InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)

            suspendCoroutine { continuation ->
                specimenIdRecognizer.process(inputImage).addOnSuccessListener { visionText: Text ->
                    val specimenId: String = visionText.text
                    onSpecimenIdUpdated(specimenId)
                }.addOnCompleteListener {
                    continuation.resume(Unit)
                }
            }

            val detection = detector.detect(image.toUprightBitmap())
            onDetectionUpdated(detection)
        }.invokeOnCompletion { exception ->
            exception?.printStackTrace()

            image.close()
            if (detector is AutoCloseable) {
                detector.close()
            }
        }
    }
}