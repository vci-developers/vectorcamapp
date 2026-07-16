package com.vci.vectorcamapp.imaging.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import com.vci.vectorcamapp.core.domain.model.results.ClassifierResult
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

class TfLiteSpecimenClassifier(
    private val context: Context,
    private val filePath: String,
    threadName: String,
) : SpecimenClassifier {

    private var model: CompiledModel? = null
    private var inputBuffers: List<TensorBuffer> = emptyList()
    private var outputBuffers: List<TensorBuffer> = emptyList()

    private val classifierLock = Any()
    private var isClosed = false
    private var usingGpu = false

    private val handlerThread = HandlerThread(threadName).apply { start() }
    private val handler = Handler(handlerThread.looper)

    private var inputTensorHeight = DEFAULT_TENSOR_HEIGHT
    private var inputTensorWidth = DEFAULT_TENSOR_WIDTH

    private var outputNumClasses = DEFAULT_NUM_CLASSES

    init {
        handler.post { initializeModel() }
    }

    private fun initializeModel() {
        synchronized(classifierLock) {
            if (model != null || isClosed) return

            try {
                model = createModelPreferringGpu(filePath)
                inputBuffers = model!!.createInputBuffers()
                outputBuffers = model!!.createOutputBuffers()

                resolveTensorShapes()
                warmModel()

                Log.d(
                    TAG,
                    "LiteRT CompiledModel initialized ($filePath, accelerator=${if (usingGpu) "GPU" else "CPU"})"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LiteRT CompiledModel ($filePath): ${e.message}", e)
                releaseModelLocked()
            }
        }
    }

    private fun createModelPreferringGpu(assetName: String): CompiledModel {
        return try {
            CompiledModel.create(
                context.assets,
                assetName,
                CompiledModel.Options(Accelerator.GPU),
            ).also {
                usingGpu = true
                Log.d(TAG, "CompiledModel created with GPU accelerator ($assetName)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPU CompiledModel failed for $assetName (${e.message}); falling back to CPU")
            usingGpu = false
            CompiledModel.create(
                context.assets,
                assetName,
                CompiledModel.Options(Accelerator.CPU),
            )
        }
    }

    private fun resolveTensorShapes() {
        try {
            val inputType = model?.getInputTensorType(INPUT_TENSOR_NAME, SIGNATURE)
            val inputDims = inputType?.layout?.dimensions
            if (inputDims != null && inputDims.size >= 4) {
                // NCHW: [1, C, H, W]
                inputTensorHeight = inputDims[2]
                inputTensorWidth = inputDims[3]
            }

            val outputType = model?.getOutputTensorType(OUTPUT_TENSOR_NAME, SIGNATURE)
            val outputDims = outputType?.layout?.dimensions
            if (outputDims != null && outputDims.size >= 2) {
                // [1, numClasses]
                outputNumClasses = outputDims[1]
            }
        } catch (e: Exception) {
            Log.w(TAG, "Using default tensor shapes for $filePath: ${e.message}")
        }
    }

    private fun isReady(): Boolean = synchronized(classifierLock) {
        !isClosed && model != null && inputBuffers.isNotEmpty() && outputBuffers.isNotEmpty()
    }

    override fun getInputTensorShape(): Pair<Int, Int> = inputTensorHeight to inputTensorWidth

    override fun getOutputTensorShape(): Int = outputNumClasses

    override suspend fun classify(croppedBitmap: Bitmap): ClassifierResult? {
        if (!isReady()) return null

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

                    val logits = synchronized(classifierLock) {
                        if (!isReady()) return@post continuation.resume(null)

                        inputBuffers[0].writeFloat(chwArray)
                        model!!.run(inputBuffers, outputBuffers)
                        outputBuffers[0].readFloat().toList()
                    }

                    Log.d(TAG, "Inference result: $logits")
                    continuation.resume(
                        ClassifierResult(
                            logits = logits,
                            inferenceDuration = System.currentTimeMillis() - startTime
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Inference failed: ${e.message}")
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
        if (output.isNotEmpty()) {
            outputNumClasses = output.size
        }
        Log.d(TAG, "Classifier warmed up ($filePath, gpu=$usingGpu, classes=$outputNumClasses)")
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
        synchronized(classifierLock) {
            if (isClosed) return
            isClosed = true

            handler.post {
                try {
                    synchronized(classifierLock) {
                        releaseModelLocked()
                    }
                    handlerThread.quitSafely()
                    Log.d(TAG, "Classifier closed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during classifier close: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "TfLiteSpeciesClassifier"
        private const val SIGNATURE = "serving_default"
        private const val INPUT_TENSOR_NAME = "serving_default_args_0:0"
        private const val OUTPUT_TENSOR_NAME = "output_0"
        private const val INPUT_CHANNELS = 3

        private const val DEFAULT_TENSOR_HEIGHT = 512
        private const val DEFAULT_TENSOR_WIDTH = 512
        private const val DEFAULT_NUM_CLASSES = 1

        private const val PIXEL_NORMALIZATION_SCALE = 1f / 255f
        private val NORMALIZE_MEAN = Scalar(0.485, 0.456, 0.406)
        private val NORMALIZE_STDDEV = Scalar(0.229, 0.224, 0.225)
    }
}
