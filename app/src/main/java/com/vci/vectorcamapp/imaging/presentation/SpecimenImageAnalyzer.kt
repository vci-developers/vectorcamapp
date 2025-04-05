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
import com.vci.vectorcamapp.imaging.domain.SpecimenDetector
import com.vci.vectorcamapp.imaging.presentation.extensions.resizeTo
import com.vci.vectorcamapp.imaging.presentation.extensions.toBoundingBoxUi
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SpecimenImageAnalyzer(
    private val detector: SpecimenDetector,
    private val onBoundingBoxUiUpdated: (BoundingBoxUi?) -> Unit,
    private val onSpecimenIdUpdated: (String) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val specimenIdRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class) // TODO: REMOVE WHEN POSSIBLE
    override fun analyze(image: ImageProxy) {
        scope.launch {
            try {
                val mediaImage: Image = image.image ?: throw IllegalStateException("No image data")
                val inputImage =
                    InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)

                suspendCoroutine { continuation ->
                    specimenIdRecognizer.process(inputImage)
                        .addOnSuccessListener { visionText: Text ->
                            onSpecimenIdUpdated(visionText.text)
                        }.addOnFailureListener { exception ->
                            Log.e("Analyzer", "Text recognition failed: ${exception.message}")
                        }.addOnCompleteListener {
                            continuation.resume(Unit)
                        }
                }

                val (tensorWidth, tensorHeight) = detector.getInputTensorShape()
                val bitmap = image.toUprightBitmap()
                val resized = bitmap.resizeTo(tensorWidth, tensorHeight)
                val boundingBox = detector.detect(resized)

                withContext(Dispatchers.Main) {
                    onBoundingBoxUiUpdated(
                        boundingBox?.toBoundingBoxUi(
                            tensorWidth, tensorHeight, bitmap.width, bitmap.height
                        )
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d("Analyzer", "Analysis cancelled.")
                } else {
                    Log.e("Analyzer", "Exception during analysis: ${e.message}")
                }
            } finally {
                image.close()
            }
        }
    }

    override fun close() {
        scope.cancel()
        specimenIdRecognizer.close()
    }
}
