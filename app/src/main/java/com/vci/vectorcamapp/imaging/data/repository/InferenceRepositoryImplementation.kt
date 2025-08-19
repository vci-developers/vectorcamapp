package com.vci.vectorcamapp.imaging.data.repository

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.results.ClassifierResult
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
                    Log.e("Repository", "Text recognition failed: ${exception.message}")
                    continuation.resume("")
                }
            }
        } catch (e: Exception) {
            Log.e("Repository", "Specimen ID analysis exception: ${e.message}", e)
            ""
        }
    }

    override suspend fun detectSpecimen(bitmap: Bitmap): List<InferenceResult> =
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
        detections: List<InferenceResult>
    ): Offset? = withContext(Dispatchers.Default) {
        if (detections.isEmpty()) return@withContext null

        val det = detections.maxByOrNull { it.bboxConfidence } ?: return@withContext null

        val x1 = (det.bboxTopLeftX * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y1 = (det.bboxTopLeftY * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val x2 = ((det.bboxTopLeftX + det.bboxWidth) * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val y2 = ((det.bboxTopLeftY + det.bboxHeight) * bitmap.height).toInt().coerceIn(0, bitmap.height)
        if (x2 <= x1 || y2 <= y1) return@withContext null

        val cropped = Bitmap.createBitmap(bitmap, x1, y1, x2 - x1, y2 - y1)

        val mat = Mat()
        Utils.bitmapToMat(cropped, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        val thresh = Mat()
        Imgproc.threshold(
            gray, thresh, 0.0, 255.0,
            Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU
        )

        val nonzero = Mat()
        Core.findNonZero(thresh, nonzero)
        if (nonzero.empty()) return@withContext null

        val n = nonzero.rows()
        val xs = IntArray(n)
        val ys = IntArray(n)
        for (i in 0 until n) {
            val p = nonzero.get(i, 0)
            xs[i] = p[0].toInt()
            ys[i] = p[1].toInt()
        }
        xs.sort(); ys.sort()
        val medianX = xs[xs.size / 2]
        val medianY = ys[ys.size / 2]

        val absX = x1 + medianX
        val absY = y1 + medianY
        val normX = absX.toFloat() / bitmap.width
        val normY = absY.toFloat() / bitmap.height

        Log.d("Repository", "AF centroid (norm)=($normX,$normY)")
        Offset(normX, normY)
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