package com.vci.vectorcamapp.imaging.data.repository

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.results.ClassifierResult
import com.vci.vectorcamapp.core.domain.model.results.DetectorResult
import com.vci.vectorcamapp.imaging.data.GpuDelegateManager
import com.vci.vectorcamapp.imaging.di.AbdomenStatusClassifier
import com.vci.vectorcamapp.imaging.di.Detector
import com.vci.vectorcamapp.imaging.di.SexClassifier
import com.vci.vectorcamapp.imaging.di.SpeciesClassifier
import com.vci.vectorcamapp.imaging.di.SpecimenIdRecognizer
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
import com.vci.vectorcamapp.imaging.domain.SpecimenDetector
import com.vci.vectorcamapp.imaging.domain.repository.InferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class InferenceRepositoryImplementation @Inject constructor(
    @SpecimenIdRecognizer private val specimenIdRecognizer: TextRecognizer,
    @Detector private val specimenDetector: SpecimenDetector,
    @SpeciesClassifier private val speciesClassifier: SpecimenClassifier,
    @SexClassifier private val sexClassifier: SpecimenClassifier,
    @AbdomenStatusClassifier private val abdomenStatusClassifier: SpecimenClassifier,
) : InferenceRepository {

    override suspend fun readSpecimenId(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            suspendCoroutine { continuation ->
                specimenIdRecognizer.process(inputImage).addOnSuccessListener { visionText ->
                    val id = visionText.text.lineSequence().firstOrNull()?.trim().orEmpty()
                    continuation.resume(id)
                }.addOnFailureListener { exception ->
                    Timber.tag("Repository").e("Text recognition failed: ${exception.message}")
                    continuation.resume("")
                }
            }
        } catch (e: Exception) {
            Timber.tag("Repository").e(e, "Specimen ID analysis exception: ${e.message}")
            ""
        }
    }

    override suspend fun detectSpecimen(bitmap: Bitmap): List<DetectorResult> =
        withContext(Dispatchers.Default) {
            specimenDetector.detect(bitmap)
        }

    override suspend fun classifySpecimen(croppedBitmap: Bitmap): Triple<ClassifierResult?, ClassifierResult?, ClassifierResult?> =
        withContext(Dispatchers.Default) {
            val speciesResultPromise = async { getClassification(croppedBitmap, speciesClassifier) }
            val sexResultPromise = async { getClassification(croppedBitmap, sexClassifier) }
            val abdomenStatusResultPromise = async { getClassification(croppedBitmap, abdomenStatusClassifier) }
            
            val speciesResult = speciesResultPromise.await()
            val sexResult = sexResultPromise.await()
            val abdomenStatusResult = abdomenStatusResultPromise.await()

            Triple(speciesResult, sexResult, abdomenStatusResult)
        }

    override suspend fun computeAutofocusCentroid(
        bitmap: Bitmap,
        detection: InferenceResult
    ): Offset? = withContext(Dispatchers.Default) {

        val topLeftX = (detection.bboxTopLeftX * bitmap.width).toInt()
        val topLeftY = (detection.bboxTopLeftY * bitmap.height).toInt()
        val cropWidth = (detection.bboxWidth * bitmap.width).toInt()
        val cropHeight = (detection.bboxHeight * bitmap.height).toInt()

        val validCropWidth = (topLeftX + cropWidth).coerceAtMost(bitmap.width) - topLeftX
        val validCropHeight = (topLeftY + cropHeight).coerceAtMost(bitmap.height) - topLeftY

        if (validCropWidth <= 0 || validCropHeight <= 0) {
            return@withContext null
        }

        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            topLeftX,
            topLeftY,
            validCropWidth,
            validCropHeight
        )

        val sourceImageMatrix = Mat()
        Utils.bitmapToMat(croppedBitmap, sourceImageMatrix)

        val grayscaleImageMatrix = Mat()
        Imgproc.cvtColor(sourceImageMatrix, grayscaleImageMatrix, Imgproc.COLOR_BGR2GRAY)

        val thresholdForegroundMaskMatrix = Mat()
        Imgproc.threshold(
            grayscaleImageMatrix, thresholdForegroundMaskMatrix, 0.0, 255.0,
            Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU
        )

        val nonZeroLocationsMatrix = Mat()
        Core.findNonZero(thresholdForegroundMaskMatrix, nonZeroLocationsMatrix)
        if (nonZeroLocationsMatrix.empty()) return@withContext null

        val foregroundPixelCount = nonZeroLocationsMatrix.rows()
        val foregroundXCoordinates = IntArray(foregroundPixelCount)
        val foregroundYCoordinates = IntArray(foregroundPixelCount)
        for (i in 0 until foregroundPixelCount) {
            val pixelLocation = nonZeroLocationsMatrix.get(i, 0)
            foregroundXCoordinates[i] = pixelLocation[0].toInt()
            foregroundYCoordinates[i] = pixelLocation[1].toInt()
        }
        foregroundXCoordinates.sort(); foregroundYCoordinates.sort()
        val medianX = foregroundXCoordinates[foregroundXCoordinates.size / 2]
        val medianY = foregroundYCoordinates[foregroundYCoordinates.size / 2]

        val absolutePixelX = topLeftX + medianX
        val absolutePixelY = topLeftY + medianY
        val normalizedFocusX = absolutePixelX.toFloat() / bitmap.width
        val normalizedFocusY = absolutePixelY.toFloat() / bitmap.height

        Timber.tag("Repository").d("AF centroid (norm)=($normalizedFocusX,$normalizedFocusY)")
        Offset(normalizedFocusX, normalizedFocusY)
    }

    private suspend fun getClassification(
        croppedBitmap: Bitmap, classifier: SpecimenClassifier
    ): ClassifierResult? = classifier.classify(croppedBitmap)

    override fun closeResources() {
        specimenIdRecognizer.close()
        specimenDetector.close()
        speciesClassifier.close()
        sexClassifier.close()
        abdomenStatusClassifier.close()
        GpuDelegateManager.close()
    }
}