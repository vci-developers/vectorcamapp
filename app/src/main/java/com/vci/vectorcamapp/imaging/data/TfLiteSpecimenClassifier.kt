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

    private val classifierLock = Any()
    private val handlerThread = HandlerThread(threadName).apply { start() }
    private val handler = Handler(handlerThread.looper)

    private var model: CompiledModel? = null
    private var inputBuffers: List<TensorBuffer> = emptyList()
    private var outputBuffers: List<TensorBuffer> = emptyList()
    private var isClosed = false

    private var inputLayout = TensorLayout.NCHW
    private var inputTensorHeight = DEFAULT_TENSOR_SIZE
    private var inputTensorWidth = DEFAULT_TENSOR_SIZE
    private var outputNumClasses = expectedNumClasses

    init {
        handler.post { initializeModel() }
    }

    override fun getInputTensorShape(): Pair<Int, Int> = inputTensorHeight to inputTensorWidth

    override fun getOutputTensorShape(): Int = outputNumClasses

    override suspend fun classify(croppedBitmap: Bitmap): ClassifierResult? {
        if (!isReady()) return null

        return suspendCoroutine { continuation ->
            handler.post {
                var rgbMatrix: Mat? = null
                var preprocessedMatrix: Mat? = null

                try {
                    val startTime = System.currentTimeMillis()

                    rgbMatrix = toRgbMatrix(croppedBitmap)
                    preprocessedMatrix = preprocess(rgbMatrix)
                    val inputValues = toModelInput(preprocessedMatrix)

                    val logits = synchronized(classifierLock) {
                        if (!isReady()) return@post continuation.resume(null)

                        inputBuffers[0].writeFloat(inputValues)
                        model!!.run(inputBuffers, outputBuffers)
                        outputBuffers[0].readFloat().take(outputNumClasses)
                    }

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

    override fun close() {
        synchronized(classifierLock) {
            if (isClosed) return
            isClosed = true

            handler.post {
                try {
                    synchronized(classifierLock) { releaseModelLocked() }
                    handlerThread.quitSafely()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to close $filePath")
                }
            }
        }
    }

    private fun initializeModel() {
        synchronized(classifierLock) {
            if (model != null || isClosed) return

            try {
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
                        "$outputNumClasses classes, accelerator=${selection.variantName}"
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize $filePath")
                releaseModelLocked()
            }
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

    private fun isReady(): Boolean = synchronized(classifierLock) {
        !isClosed && model != null && inputBuffers.isNotEmpty() && outputBuffers.isNotEmpty()
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

    private fun releaseModelLocked() {
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
