package com.vci.vectorcamapp.imaging.presentation

import android.media.Image
import android.util.Log
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
) : ImageAnalysis.Analyzer, AutoCloseable {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val specimenIdRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class) // TODO - Remove when possible
    override fun analyze(image: ImageProxy) {
        scope.launch {
            val mediaImage: Image = image.image ?: run {
                Log.e("EXCEPTION", "ERROR")
                image.close()
                return@launch
            }
            val inputImage: InputImage =
                InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)

            suspendCoroutine { continuation ->
                specimenIdRecognizer.process(inputImage).addOnSuccessListener { visionText: Text ->
                    val specimenId: String = visionText.text
                    onSpecimenIdUpdated(specimenId)
                }.addOnFailureListener { exception ->
                    Log.e("EXCEPTION", exception.message.toString())
                }.addOnCompleteListener {
                    continuation.resume(Unit)
                }
            }

            val tensorWidth = detector.getInputTensorShape().first
            val tensorHeight = detector.getInputTensorShape().second

            val bitmap = image.toUprightBitmap()
            val detection = detector.detect(bitmap.resizeTo(tensorWidth, tensorHeight))

            onDetectionUpdated(
                detection?.toDetection(
                    tensorWidth, tensorHeight, bitmap.width, bitmap.height
                )
            )

        }.invokeOnCompletion { exception ->
            exception?.let { e ->
                Log.e("EXCEPTION", e.message.toString())
            }

            image.close()
        }
    }

    override fun close() {
        detector.close()
        specimenIdRecognizer.close()
    }
}
