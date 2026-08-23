package com.vci.vectorcamapp.imaging.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import com.vci.vectorcamapp.core.domain.model.results.DetectorResult
import com.vci.vectorcamapp.imaging.data.util.GpuAccelerationPolicy
import com.vci.vectorcamapp.imaging.domain.SpecimenDetector
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfRect2d
import org.opencv.core.Rect
import org.opencv.core.Rect2d
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.math.roundToInt

class TfLiteSpecimenDetector(
    private val context: Context
) : SpecimenDetector {

    private var model: CompiledModel? = null
    private var inputBuffers: List<TensorBuffer> = emptyList()
    private var outputBuffers: List<TensorBuffer> = emptyList()

    private val detectorLock = Any()
    private var isClosed = false
    private var usingGpu = false

    private val handlerThread = HandlerThread("LiteRTSpecimenDetectorThread").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private var inputTensorHeight = DEFAULT_TENSOR_HEIGHT
    private var inputTensorWidth = DEFAULT_TENSOR_WIDTH

    private var outputNumChannels = DEFAULT_NUM_CHANNELS
    private var outputNumElements = DEFAULT_NUM_ELEMENTS

    init {
        handler.post { initializeModel() }
    }

    private fun initializeModel() {
        synchronized(detectorLock) {
            if (model != null || isClosed) return

            try {
                val startTime = System.currentTimeMillis()
                model = createModelPreferringGpu(MODEL_ASSET)
                inputBuffers = model!!.createInputBuffers()
                outputBuffers = model!!.createOutputBuffers()

                resolveTensorShapes()
                warmModel()

                if (usingGpu) {
                    val elapsedMs = System.currentTimeMillis() - startTime
                    GpuAccelerationPolicy.recordGpuWarmup(context, elapsedMs, succeeded = true)

                    if (!GpuAccelerationPolicy.shouldAttemptGpu(context)) {
                        Timber.w(
                            "GPU warm-up too slow (${elapsedMs}ms) on this device; " +
                                "rebuilding detector on CPU"
                        )
                        releaseModelLocked()
                        model = createModelCpuOnly(MODEL_ASSET)
                        inputBuffers = model!!.createInputBuffers()
                        outputBuffers = model!!.createOutputBuffers()
                        resolveTensorShapes()
                        warmModel()
                    }
                }

                Timber.d(
                    "LiteRT CompiledModel initialized (accelerator=${if (usingGpu) "GPU" else "CPU"})"
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize LiteRT CompiledModel: ${e.message}")
                releaseModelLocked()
            }
        }
    }

    private fun createModelPreferringGpu(assetName: String): CompiledModel {
        if (!GpuAccelerationPolicy.shouldAttemptGpu(context)) {
            Timber.w("GPU accelerator skipped for live camera on this device tier; using CPU")
            return createModelCpuOnly(assetName)
        }

        val startTime = System.currentTimeMillis()
        return try {
            CompiledModel.create(
                context.assets,
                assetName,
                CompiledModel.Options(Accelerator.GPU),
            ).also {
                usingGpu = true
                Timber.d("CompiledModel created with GPU accelerator")
            }
        } catch (e: Exception) {
            Timber.w("GPU CompiledModel failed (${e.message}); falling back to CPU")
            GpuAccelerationPolicy.recordGpuWarmup(
                context,
                System.currentTimeMillis() - startTime,
                succeeded = false
            )
            createModelCpuOnly(assetName)
        }
    }

    private fun createModelCpuOnly(assetName: String): CompiledModel {
        usingGpu = false
        return CompiledModel.create(
            context.assets,
            assetName,
            CompiledModel.Options(Accelerator.CPU),
        )
    }

    private fun resolveTensorShapes() {
        val compiled = model ?: return

        try {
            val inputDims = compiled.getInputTensorType(INPUT_TENSOR_NAME, SIGNATURE).layout?.dimensions
            if (inputDims != null && inputDims.size >= 3) {
                // NHWC: [1, H, W, C]
                inputTensorHeight = inputDims[1]
                inputTensorWidth = inputDims[2]
                Timber.d("Input tensor type: $inputDims")
            }
        } catch (e: Exception) {
            Timber.w("getInputTensorType failed: ${e.message}")
        }

        try {
            val floatCount =
                compiled.getInputBufferRequirements(INPUT_TENSOR_NAME, SIGNATURE).bufferSize /
                    Float.SIZE_BYTES
            val spatial = floatCount / INPUT_CHANNELS
            val side = kotlin.math.sqrt(spatial.toDouble()).toInt()
            if (side > 0 && side * side * INPUT_CHANNELS == floatCount) {
                inputTensorHeight = side
                inputTensorWidth = side
            }
            Timber.d(
                "Resolved input ${inputTensorWidth}x$inputTensorHeight (buffer floats=$floatCount)"
            )
        } catch (e: Exception) {
            Timber.w("getInputBufferRequirements failed: ${e.message}")
        }

        try {
            val outputDims =
                compiled.getOutputTensorType(OUTPUT_TENSOR_NAME, SIGNATURE).layout?.dimensions
            if (outputDims != null && outputDims.size >= 3) {
                // [1, numChannels, numElements]
                outputNumChannels = outputDims[1]
                outputNumElements = outputDims[2]
            }
        } catch (e: Exception) {
            Timber.w("getOutputTensorType failed: ${e.message}")
        }
    }

    override fun getInputTensorShape(): Pair<Int, Int> = inputTensorHeight to inputTensorWidth

    override fun getOutputTensorShape(): Pair<Int, Int> = outputNumChannels to outputNumElements

    override suspend fun detect(bitmap: Bitmap): List<DetectorResult> {
        if (!isReady()) return emptyList()

        return suspendCoroutine { continuation ->
            handler.post {
                try {
                    val startTime = System.currentTimeMillis()
                    val inputMatrix = prepareInputMatrix(bitmap)

                    val preprocessedMatrix = preprocessMatrix(inputMatrix)
                    val preprocessedMatrixHeight = preprocessedMatrix.height()
                    val preprocessedMatrixWidth = preprocessedMatrix.width()
                    val preprocessedMatrixChannels = preprocessedMatrix.channels()

                    val inputFloatBuffer =
                        FloatArray(preprocessedMatrixHeight * preprocessedMatrixWidth * preprocessedMatrixChannels)
                    preprocessedMatrix.get(0, 0, inputFloatBuffer)

                    val result = synchronized(detectorLock) {
                        if (!isReady()) return@post continuation.resume(emptyList())

                        inputBuffers[0].writeFloat(inputFloatBuffer)
                        model!!.run(inputBuffers, outputBuffers)
                        val output = outputBuffers[0].readFloat()

                        getDetectedResults(output).map { bboxPrediction ->
                            DetectorResult(
                                bboxTopLeftX = bboxPrediction[0],
                                bboxTopLeftY = bboxPrediction[1],
                                bboxWidth = bboxPrediction[2],
                                bboxHeight = bboxPrediction[3],
                                bboxConfidence = bboxPrediction[4],
                                bboxClassId = bboxPrediction[5].roundToInt(),
                                bboxDetectionDuration = System.currentTimeMillis() - startTime
                            )
                        }
                    }

                    continuation.resume(result)
                } catch (e: Exception) {
                    Timber.e("Inference failed: ${e.message}")
                    continuation.resume(emptyList())
                }
            }
        }
    }

    private fun isReady(): Boolean = synchronized(detectorLock) {
        !isClosed && model != null && inputBuffers.isNotEmpty() && outputBuffers.isNotEmpty()
    }

    private fun prepareInputMatrix(bitmap: Bitmap): Mat {
        val inputMatrix = Mat()
        Utils.bitmapToMat(bitmap, inputMatrix)
        Imgproc.cvtColor(inputMatrix, inputMatrix, Imgproc.COLOR_RGBA2BGR)
        return inputMatrix
    }

    private fun preprocessMatrix(inputMatrix: Mat): Mat {
        val resizedMatrix = Mat()
        val resizedMatrixWidth = (inputTensorWidth / ASPECT_RATIO).toInt()
        val resizedMatrixHeight = inputTensorHeight
        Imgproc.resize(
            inputMatrix,
            resizedMatrix,
            Size(resizedMatrixWidth.toDouble(), resizedMatrixHeight.toDouble())
        )

        val paddedSideLength = max(resizedMatrixWidth, resizedMatrixHeight)
        val paddedMatrix =
            Mat(paddedSideLength, paddedSideLength, resizedMatrix.type(), Scalar(0.0, 0.0, 0.0))

        val xOffset = (paddedSideLength - resizedMatrixWidth) / 2
        val yOffset = (paddedSideLength - resizedMatrixHeight) / 2
        val regionOfIntersection = paddedMatrix.submat(
            Rect(xOffset, yOffset, resizedMatrixWidth, resizedMatrixHeight)
        )
        resizedMatrix.copyTo(regionOfIntersection)
        paddedMatrix.convertTo(paddedMatrix, CvType.CV_32F, PIXEL_NORMALIZATION_SCALE.toDouble())
        return paddedMatrix
    }

    private fun getDetectedResults(boxes: FloatArray): List<FloatArray> {
        val predictions = mutableListOf<FloatArray>()

        for (i in 0 until outputNumChannels) {
            val prediction = FloatArray(outputNumElements)
            for (j in 0 until outputNumElements) {
                prediction[j] = boxes[i * outputNumElements + j]
            }
            if (prediction[4] >= CONFIDENCE_THRESHOLD) {
                predictions.add(prediction)
            }
        }

        if (predictions.isEmpty()) return emptyList()

        val boxesForNms = mutableListOf<Rect2d>()
        val confidenceScoresForNms = mutableListOf<Float>()

        for (prediction in predictions) {
            val centerXNormalized = prediction[0]
            val centerYNormalized = prediction[1]
            val widthNormalized = prediction[2]
            val heightNormalized = prediction[3]
            val confidence = prediction[4]

            val topLeftXAbsolute = (centerXNormalized - (widthNormalized / 2f)) * inputTensorWidth
            val topLeftYAbsolute = (centerYNormalized - (heightNormalized / 2f)) * inputTensorHeight
            val widthAbsolute = widthNormalized * inputTensorWidth
            val heightAbsolute = heightNormalized * inputTensorHeight

            boxesForNms.add(
                Rect2d(
                    topLeftXAbsolute.toDouble(),
                    topLeftYAbsolute.toDouble(),
                    widthAbsolute.toDouble(),
                    heightAbsolute.toDouble()
                )
            )
            confidenceScoresForNms.add(confidence)
        }

        val boxesMatrix = MatOfRect2d(*boxesForNms.toTypedArray())
        val confidenceScoresMatrix = MatOfFloat(*confidenceScoresForNms.toFloatArray())
        val indicesMatrix = MatOfInt()
        Dnn.NMSBoxes(
            boxesMatrix, confidenceScoresMatrix, CONFIDENCE_THRESHOLD, IOU_THRESHOLD, indicesMatrix
        )

        if (indicesMatrix.empty()) return emptyList()

        val selectedIndices = indicesMatrix.toArray()
        val finalBboxPredictions = mutableListOf<FloatArray>()

        for (index in selectedIndices) {
            val prediction = predictions[index]

            var centerX = prediction[0]
            val centerY = prediction[1]
            var width = prediction[2]
            val height = prediction[3]
            val confidence = prediction[4]
            val classId = prediction[5]

            val offset =
                ((inputTensorWidth - (inputTensorWidth / ASPECT_RATIO)) / 2f) / inputTensorWidth
            centerX -= offset
            centerX *= ASPECT_RATIO
            width *= ASPECT_RATIO

            val topLeftX = (centerX - width / 2f).coerceAtLeast(0f)
            val topLeftY = (centerY - height / 2f).coerceAtLeast(0f)
            Timber.d("InferenceResult: ($topLeftX, $topLeftY) -> ($width, $height)")

            finalBboxPredictions.add(floatArrayOf(topLeftX, topLeftY, width, height, confidence, classId))
        }

        return finalBboxPredictions
    }

    private fun warmModel() {
        val inputSize = inputTensorHeight * inputTensorWidth * INPUT_CHANNELS
        inputBuffers[0].writeFloat(FloatArray(inputSize))
        model?.run(inputBuffers, outputBuffers)
        val output = outputBuffers[0].readFloat()
        if (output.isNotEmpty() && output.size % outputNumElements == 0) {
            outputNumChannels = output.size / outputNumElements
        }
        Timber.d("Detector warmed up (gpu=$usingGpu, outputChannels=$outputNumChannels)")
    }

    private fun releaseModelLocked() {
        inputBuffers.forEach { buffer ->
            try {
                buffer.close()
            } catch (_: Exception) {
            }
        }
        outputBuffers.forEach { buffer ->
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

    override fun close() {
        synchronized(detectorLock) {
            if (isClosed) return
            isClosed = true

            handler.post {
                try {
                    synchronized(detectorLock) {
                        releaseModelLocked()
                    }
                    handlerThread.quitSafely()
                    Timber.d("Detector closed")
                } catch (e: Exception) {
                    Timber.e("Error during detector close: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val MODEL_ASSET = "detect.tflite"
        private const val SIGNATURE = "serving_default"

        // Signature input/output names rather than tensor names - see TfLiteSpecimenClassifier.
        private const val INPUT_TENSOR_NAME = "keras_tensor_121"
        private const val OUTPUT_TENSOR_NAME = "output_0"
        private const val INPUT_CHANNELS = 3

        private const val DEFAULT_TENSOR_HEIGHT = 640
        private const val DEFAULT_TENSOR_WIDTH = 640
        private const val DEFAULT_NUM_CHANNELS = 25200
        private const val DEFAULT_NUM_ELEMENTS = 6
        private const val CONFIDENCE_THRESHOLD = 0.2f
        private const val IOU_THRESHOLD = 0.5f

        private const val PIXEL_NORMALIZATION_SCALE = 1f / 255f
        private const val ASPECT_RATIO = 4f / 3f
    }
}
