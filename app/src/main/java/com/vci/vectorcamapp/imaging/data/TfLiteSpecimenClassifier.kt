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
) : SpecimenClassifier {

    // Only touched on [handler]'s thread, which is the single thread every build, inference and
    // teardown is posted to. That confinement is what keeps callers off a lock during the build.
    private var model: CompiledModel? = null
    private var inputBuffers: List<TensorBuffer> = emptyList()
    private var outputBuffers: List<TensorBuffer> = emptyList()
    private var usingGpu = false

    private val stateLock = Any()
    private var warmUp: CompletableDeferred<Boolean>? = null
    private var isClosed = false

    private val handlerThread = HandlerThread(threadName).apply { start() }
    private val handler = Handler(handlerThread.looper)

    @Volatile
    private var inputTensorHeight = DEFAULT_TENSOR_HEIGHT

    @Volatile
    private var inputTensorWidth = DEFAULT_TENSOR_WIDTH

    @Volatile
    private var outputNumClasses = DEFAULT_NUM_CLASSES

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
            model = selection.model
            usingGpu = selection.usingGpu
            inputBuffers = selection.model.createInputBuffers()
            outputBuffers = selection.model.createOutputBuffers()

            resolveTensorShapes()
            warmModel()

            Timber.d(
                "LiteRT CompiledModel initialized ($filePath, " +
                    "accelerator=${selection.variantName}, " +
                    "${System.currentTimeMillis() - startTime}ms)"
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize LiteRT CompiledModel ($filePath): ${e.message}")
            releaseModel()
            false
        }
    }

    private fun resolveTensorShapes() {
        val compiled = model ?: return
        var inputResolved = false

        try {
            val inputDims = compiled.getInputTensorType(INPUT_TENSOR_NAME, SIGNATURE).layout?.dimensions
            if (inputDims != null && inputDims.size >= 4) {
                // NCHW: [1, C, H, W]
                inputTensorHeight = inputDims[2]
                inputTensorWidth = inputDims[3]
                inputResolved = true
                Timber.d("Input tensor type for $filePath: $inputDims")
            }
        } catch (e: Exception) {
            Timber.e(e, "getInputTensorType failed for $filePath: ${e.message}")
        }

        // Buffer requirements are authoritative for writeFloat sizing.
        try {
            val floatCount =
                compiled.getInputBufferRequirements(INPUT_TENSOR_NAME, SIGNATURE).bufferSize /
                    Float.SIZE_BYTES
            val spatial = floatCount / INPUT_CHANNELS
            val side = kotlin.math.sqrt(spatial.toDouble()).toInt()
            if (side > 0 && side * side * INPUT_CHANNELS == floatCount) {
                inputTensorHeight = side
                inputTensorWidth = side
                inputResolved = true
            }
            Timber.d(
                "Resolved input ${inputTensorWidth}x$inputTensorHeight for $filePath " +
                    "(buffer floats=$floatCount)"
            )
        } catch (e: Exception) {
            Timber.e(e, "getInputBufferRequirements failed for $filePath: ${e.message}")
        }

        // Falling back to the defaults silently feeds the model the wrong resolution, which yields
        // confident but meaningless predictions rather than an outright failure.
        if (!inputResolved) {
            Timber.e(
                "Could not resolve input shape for $filePath; " +
                    "classifying at fallback ${inputTensorWidth}x$inputTensorHeight"
            )
        }

        try {
            val outputDims =
                compiled.getOutputTensorType(OUTPUT_TENSOR_NAME, SIGNATURE).layout?.dimensions
            if (outputDims != null && outputDims.size >= 2) {
                outputNumClasses = outputDims[1]
            }
        } catch (e: Exception) {
            Timber.w("getOutputTensorType failed for $filePath: ${e.message}")
        }
    }

    override fun getInputTensorShape(): Pair<Int, Int> = inputTensorHeight to inputTensorWidth

    override fun getOutputTensorShape(): Int = outputNumClasses

    override suspend fun classify(croppedBitmap: Bitmap): ClassifierResult? {
        // All three classifiers are called in parallel from Dispatchers.Default, so waiting on the
        // build has to suspend: blocking here would park three of that pool's threads for as long
        // as the build takes.
        if (!startWarmUp().await()) return null

        return suspendCoroutine { continuation ->
            handler.post {
                try {
                    val startTime = System.currentTimeMillis()
                    val inputMatrix = prepareInputMatrix(croppedBitmap)

                    val preprocessedMatrix = preprocessMatrix(inputMatrix)
                    val preprocessedMatrixHeight = preprocessedMatrix.height()
                    val preprocessedMatrixWidth = preprocessedMatrix.width()
                    val preprocessedMatrixChannels = preprocessedMatrix.channels()

                    val inputFloatBuffer =
                        FloatArray(preprocessedMatrixHeight * preprocessedMatrixWidth * preprocessedMatrixChannels)
                    preprocessedMatrix.get(0, 0, inputFloatBuffer)

                    val chwArray =
                        FloatArray(preprocessedMatrixChannels * preprocessedMatrixHeight * preprocessedMatrixWidth)
                    for (channel in 0 until preprocessedMatrixChannels) {
                        for (i in 0 until preprocessedMatrixHeight * preprocessedMatrixWidth) {
                            chwArray[channel * preprocessedMatrixHeight * preprocessedMatrixWidth + i] =
                                inputFloatBuffer[i * preprocessedMatrixChannels + channel]
                        }
                    }

                    // A release can land between the build completing and this runnable, so the
                    // model has to be re-checked here rather than trusted from the warm-up.
                    val compiled = model
                        ?: return@post continuation.resume(null)

                    inputBuffers[0].writeFloat(chwArray)
                    compiled.run(inputBuffers, outputBuffers)
                    val logits = outputBuffers[0].readFloat().take(outputNumClasses)

                    Timber.d("Inference result: $logits")
                    continuation.resume(
                        ClassifierResult(
                            logits = logits,
                            inferenceDuration = System.currentTimeMillis() - startTime
                        )
                    )
                } catch (e: Exception) {
                    Timber.e("Inference failed: ${e.message}")
                    continuation.resume(null)
                }
            }
        }
    }

    private fun prepareInputMatrix(croppedBitmap: Bitmap): Mat {
        val inputMatrix = Mat()
        Utils.bitmapToMat(croppedBitmap, inputMatrix)
        Imgproc.cvtColor(inputMatrix, inputMatrix, Imgproc.COLOR_RGBA2RGB)
        return inputMatrix
    }

    private fun preprocessMatrix(inputMatrix: Mat): Mat {
        val inputMatrixWidth = inputMatrix.width()
        val inputMatrixHeight = inputMatrix.height()
        val paddedSideLength = max(inputMatrixWidth, inputMatrixHeight)
        val paddedMatrix = Mat.zeros(paddedSideLength, paddedSideLength, inputMatrix.type())

        val rowStart = (paddedSideLength - inputMatrixHeight) / 2
        val rowEnd = rowStart + inputMatrixHeight
        val colStart = (paddedSideLength - inputMatrixWidth) / 2
        val colEnd = colStart + inputMatrixWidth
        val regionOfIntersection = paddedMatrix.submat(rowStart, rowEnd, colStart, colEnd)
        inputMatrix.copyTo(regionOfIntersection)

        val resizedMatrix = Mat()
        Imgproc.resize(
            paddedMatrix,
            resizedMatrix,
            Size(inputTensorWidth.toDouble(), inputTensorHeight.toDouble())
        )
        resizedMatrix.convertTo(resizedMatrix, CvType.CV_32F, PIXEL_NORMALIZATION_SCALE.toDouble())

        val meanMatrix = Mat(resizedMatrix.size(), CvType.CV_32FC3, NORMALIZE_MEAN)
        val stdDevMatrix = Mat(resizedMatrix.size(), CvType.CV_32FC3, NORMALIZE_STDDEV)
        Core.subtract(resizedMatrix, meanMatrix, resizedMatrix)
        Core.divide(resizedMatrix, stdDevMatrix, resizedMatrix)

        return resizedMatrix
    }

    private fun warmModel() {
        val inputSize = INPUT_CHANNELS * inputTensorHeight * inputTensorWidth
        inputBuffers[0].writeFloat(FloatArray(inputSize))
        model?.run(inputBuffers, outputBuffers)
        val output = outputBuffers[0].readFloat()
        // Only trust the buffer length when the model didn't report its output shape: accelerator
        // buffers can be padded beyond the real class count.
        if (output.isNotEmpty() && outputNumClasses == DEFAULT_NUM_CLASSES) {
            outputNumClasses = output.size
        }
        Timber.d(
            "Classifier warmed up ($filePath, gpu=$usingGpu, " +
                "input=${inputTensorWidth}x$inputTensorHeight, classes=$outputNumClasses)"
        )
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
        usingGpu = false
    }

    override fun release() {
        synchronized(stateLock) {
            if (isClosed || warmUp == null) return
            warmUp = null
        }

        handler.post {
            releaseModel()
            Timber.d("Classifier released ($filePath)")
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
                Timber.d("Classifier closed ($filePath)")
            } catch (e: Exception) {
                Timber.e("Error during classifier close: ${e.message}")
            }
        }
    }

    companion object {
        private const val SIGNATURE = "serving_default"

        // These are the signature's input/output names, not the underlying tensor names
        // ("serving_default_args_0:0" / "StatefulPartitionedCall:0"). LiteRT resolves shapes and
        // buffer requirements by signature name, and passing tensor names makes every lookup throw.
        private const val INPUT_TENSOR_NAME = "args_0"
        private const val OUTPUT_TENSOR_NAME = "output_0"
        private const val INPUT_CHANNELS = 3

        // Defaults only used until shapes are resolved from the model.
        // sex/abdomen_status are 300x300; species is 512x512.
        private const val DEFAULT_TENSOR_HEIGHT = 300
        private const val DEFAULT_TENSOR_WIDTH = 300
        private const val DEFAULT_NUM_CLASSES = 1

        private const val PIXEL_NORMALIZATION_SCALE = 1f / 255f
        private val NORMALIZE_MEAN = Scalar(0.485, 0.456, 0.406)
        private val NORMALIZE_STDDEV = Scalar(0.229, 0.224, 0.225)
    }
}
