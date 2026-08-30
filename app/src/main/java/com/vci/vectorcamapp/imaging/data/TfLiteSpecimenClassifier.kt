package com.vci.vectorcamapp.imaging.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import com.vci.vectorcamapp.core.domain.model.results.ClassifierResult
import com.vci.vectorcamapp.imaging.data.util.ClassifierAcceleratorSelector
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
import kotlinx.coroutines.CompletableDeferred
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

class TfLiteSpecimenClassifier(
    private val context: Context,
    private val filePath: String,
    threadName: String,
    private val expectedNumClasses: Int,
) : SpecimenClassifier {

    private enum class TensorLayout { NCHW, NHWC }

    private val handlerThread = HandlerThread(threadName).apply { start() }
    private val handler = Handler(handlerThread.looper)

    // Only touched on [handler]'s thread, which is the single thread every build, inference and
    // teardown is posted to. That confinement is what keeps callers off a lock during the build.
    private var model: CompiledModel? = null
    private var inputBuffers: List<TensorBuffer> = emptyList()
    private var outputBuffers: List<TensorBuffer> = emptyList()
    private var inputLayout = TensorLayout.NCHW

    private val stateLock = Any()
    private var warmUp: CompletableDeferred<Boolean>? = null
    private var isClosed = false

    // Read from outside the handler thread by the shape getters.
    @Volatile
    private var inputTensorHeight = DEFAULT_TENSOR_SIZE

    @Volatile
    private var inputTensorWidth = DEFAULT_TENSOR_SIZE

    @Volatile
    private var outputNumClasses = expectedNumClasses

    override fun getInputTensorShape(): Pair<Int, Int> = inputTensorHeight to inputTensorWidth

    override fun getOutputTensorShape(): Int = outputNumClasses

    override fun warm() {
        startWarmUp()
    }

    /**
     * Returns the in-flight or completed build, starting one if the model is cold. The result is
     * awaited rather than polled so a capture that lands mid-build suspends instead of holding a
     * thread.
     */
    private fun startWarmUp(): CompletableDeferred<Boolean> {
        val pending = synchronized(stateLock) {
            if (isClosed) return CompletableDeferred(false)
            warmUp?.let { return it }
            CompletableDeferred<Boolean>().also { warmUp = it }
        }

        handler.post {
            // A release posted between the request and here means nobody wants this model any
            // more, and building it only to tear it down would hold the thread for seconds.
            val stillWanted = synchronized(stateLock) { warmUp === pending }
            pending.complete(if (stillWanted) initializeModel() else false)
        }
        return pending
    }

    override suspend fun classify(croppedBitmap: Bitmap): ClassifierResult? {
        // All three classifiers are called in parallel from Dispatchers.Default, so waiting on the
        // build has to suspend: blocking here would park three of that pool's threads for as long
        // as the build takes.
        if (!startWarmUp().await()) return null

        return suspendCoroutine { continuation ->
            handler.post {
                var rgbMatrix: Mat? = null
                var preprocessedMatrix: Mat? = null

                try {
                    val startTime = System.currentTimeMillis()

                    // A release can land between the build completing and this runnable, so the
                    // model has to be re-checked here rather than trusted from the warm-up.
                    val compiledModel = model
                    if (compiledModel == null) {
                        continuation.resume(null)
                        return@post
                    }

                    rgbMatrix = toRgbMatrix(croppedBitmap)
                    preprocessedMatrix = preprocess(rgbMatrix)
                    val inputValues = toModelInput(preprocessedMatrix)

                    inputBuffers[0].writeFloat(inputValues)
                    compiledModel.run(inputBuffers, outputBuffers)
                    val logits = outputBuffers[0].readFloat().take(outputNumClasses)

                    Timber.d("$filePath logits: $logits")
                    continuation.resume(
                        ClassifierResult(
                            logits = logits,
                            inferenceDuration = System.currentTimeMillis() - startTime
                        )
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Inference failed for $filePath")
                    continuation.resume(null)
                } finally {
                    preprocessedMatrix?.release()
                    rgbMatrix?.release()
                }
            }
        }
    }

    override fun release() {
        synchronized(stateLock) {
            if (isClosed || warmUp == null) return
            warmUp = null
        }

        handler.post {
            releaseModel()
            Timber.d("Released $filePath")
        }
    }

    override fun close() {
        synchronized(stateLock) {
            if (isClosed) return
            isClosed = true
            warmUp = null
        }

        handler.post {
            try {
                releaseModel()
                handlerThread.quitSafely()
            } catch (e: Exception) {
                Timber.e(e, "Failed to close $filePath")
            }
        }
    }

    private fun initializeModel(): Boolean {
        if (model != null) return true

        return try {
            val startTime = System.currentTimeMillis()
            val selection = ClassifierAcceleratorSelector.selectModel(
                context = context,
                assetName = filePath,
                signature = SIGNATURE,
                inputTensorName = INPUT_TENSOR_NAME,
            )
            val compiledModel = selection.model
            model = compiledModel
            inputBuffers = compiledModel.createInputBuffers()
            outputBuffers = compiledModel.createOutputBuffers()

            resolveTensorShapes(compiledModel)
            warmModel()

            Timber.d(
                "Initialized $filePath: $inputLayout ${inputTensorWidth}x$inputTensorHeight, " +
                    "$outputNumClasses classes, accelerator=${selection.variantName}, " +
                    "${System.currentTimeMillis() - startTime}ms"
            )
            true
        } catch (e: Exception) {
            // Includes the shape contracts in resolveTensorShapes. Failing here leaves the
            // classifier unusable and classify() returning null, which is the point: feeding the
            // model the wrong resolution or reading the wrong class count yields confident
            // nonsense rather than an obvious failure.
            Timber.e(e, "Failed to initialize $filePath")
            releaseModel()
            false
        }
    }

    private fun resolveTensorShapes(compiledModel: CompiledModel) {
        val inputDimensions =
            compiledModel.getInputTensorType(INPUT_TENSOR_NAME, SIGNATURE).layout?.dimensions
        requireNotNull(inputDimensions) { "Missing input tensor layout for $filePath" }
        require(inputDimensions.size == 4 && inputDimensions[0] == 1) {
            "Unsupported input shape for $filePath: ${inputDimensions.joinToString()}"
        }

        when {
            inputDimensions[1] == INPUT_CHANNELS -> {
                inputLayout = TensorLayout.NCHW
                inputTensorHeight = inputDimensions[2]
                inputTensorWidth = inputDimensions[3]
            }

            inputDimensions[3] == INPUT_CHANNELS -> {
                inputLayout = TensorLayout.NHWC
                inputTensorHeight = inputDimensions[1]
                inputTensorWidth = inputDimensions[2]
            }

            else -> throw IllegalArgumentException(
                "Unsupported input shape for $filePath: ${inputDimensions.joinToString()}"
            )
        }

        val outputDimensions =
            compiledModel.getOutputTensorType(OUTPUT_TENSOR_NAME, SIGNATURE).layout?.dimensions
        requireNotNull(outputDimensions) { "Missing output tensor layout for $filePath" }
        require(outputDimensions.size == 2 && outputDimensions[0] == 1) {
            "Unsupported output shape for $filePath: ${outputDimensions.joinToString()}"
        }

        outputNumClasses = outputDimensions[1]
        require(outputNumClasses == expectedNumClasses) {
            "$filePath emits $outputNumClasses classes, expected $expectedNumClasses"
        }
    }

    private fun warmModel() {
        inputBuffers[0].writeFloat(
            FloatArray(INPUT_CHANNELS * inputTensorHeight * inputTensorWidth)
        )
        model?.run(inputBuffers, outputBuffers)
    }

    private fun toRgbMatrix(croppedBitmap: Bitmap): Mat {
        val rgbMatrix = Mat()
        Utils.bitmapToMat(croppedBitmap, rgbMatrix)
        Imgproc.cvtColor(rgbMatrix, rgbMatrix, Imgproc.COLOR_RGBA2RGB)
        return rgbMatrix
    }

    private fun preprocess(rgbMatrix: Mat): Mat {
        val squareSideLength = max(rgbMatrix.width(), rgbMatrix.height())
        val paddedMatrix = Mat.zeros(squareSideLength, squareSideLength, rgbMatrix.type())
        val rowStart = (squareSideLength - rgbMatrix.height()) / 2
        val columnStart = (squareSideLength - rgbMatrix.width()) / 2

        val centeredRegion = paddedMatrix.submat(
            rowStart,
            rowStart + rgbMatrix.height(),
            columnStart,
            columnStart + rgbMatrix.width(),
        )
        rgbMatrix.copyTo(centeredRegion)
        centeredRegion.release()

        val resizedMatrix = Mat()
        Imgproc.resize(
            paddedMatrix,
            resizedMatrix,
            Size(inputTensorWidth.toDouble(), inputTensorHeight.toDouble())
        )
        paddedMatrix.release()

        resizedMatrix.convertTo(resizedMatrix, CvType.CV_32F, PIXEL_NORMALIZATION_SCALE.toDouble())

        val meanMatrix = Mat(resizedMatrix.size(), CvType.CV_32FC3, NORMALIZE_MEAN)
        val standardDeviationMatrix = Mat(resizedMatrix.size(), CvType.CV_32FC3, NORMALIZE_STDDEV)
        Core.subtract(resizedMatrix, meanMatrix, resizedMatrix)
        Core.divide(resizedMatrix, standardDeviationMatrix, resizedMatrix)
        meanMatrix.release()
        standardDeviationMatrix.release()

        return resizedMatrix
    }

    private fun toModelInput(preprocessedMatrix: Mat): FloatArray {
        require(
            preprocessedMatrix.height() == inputTensorHeight &&
                preprocessedMatrix.width() == inputTensorWidth &&
                preprocessedMatrix.channels() == INPUT_CHANNELS
        ) {
            "Unexpected preprocessed shape for $filePath: ${preprocessedMatrix.height()}x" +
                "${preprocessedMatrix.width()}x${preprocessedMatrix.channels()}"
        }

        val hwcValues = FloatArray(inputTensorHeight * inputTensorWidth * INPUT_CHANNELS)
        preprocessedMatrix.get(0, 0, hwcValues)

        return when (inputLayout) {
            TensorLayout.NHWC -> hwcValues
            TensorLayout.NCHW -> toChannelsFirst(hwcValues)
        }
    }

    private fun toChannelsFirst(hwcValues: FloatArray): FloatArray {
        val pixelCount = inputTensorHeight * inputTensorWidth
        val chwValues = FloatArray(hwcValues.size)

        for (channel in 0 until INPUT_CHANNELS) {
            for (pixel in 0 until pixelCount) {
                chwValues[channel * pixelCount + pixel] =
                    hwcValues[pixel * INPUT_CHANNELS + channel]
            }
        }

        return chwValues
    }

    private fun releaseModel() {
        (inputBuffers + outputBuffers).forEach { buffer ->
            try {
                buffer.close()
            } catch (_: Exception) {
            }
        }
        inputBuffers = emptyList()
        outputBuffers = emptyList()

        try {
            model?.close()
        } catch (_: Exception) {
        }
        model = null
    }

    private companion object {
        const val SIGNATURE = "serving_default"
        const val INPUT_TENSOR_NAME = "args_0"
        const val OUTPUT_TENSOR_NAME = "output_0"
        const val INPUT_CHANNELS = 3
        const val DEFAULT_TENSOR_SIZE = 300
        const val PIXEL_NORMALIZATION_SCALE = 1f / 255f

        val NORMALIZE_MEAN = Scalar(0.485, 0.456, 0.406)
        val NORMALIZE_STDDEV = Scalar(0.229, 0.224, 0.225)
    }
}
