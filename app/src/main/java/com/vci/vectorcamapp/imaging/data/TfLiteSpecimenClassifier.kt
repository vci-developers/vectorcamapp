package com.vci.vectorcamapp.imaging.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.vci.vectorcamapp.core.domain.model.results.ClassifierResult
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

class TfLiteSpecimenClassifier(
    private val context: Context,
    private val filePath: String,
    threadName: String,
) : SpecimenClassifier {

    private var classifier: Interpreter? = null

    private val classifierLock = Any()
    private var isClosed = false

    private val handlerThread = HandlerThread(threadName).apply { start() }
    private val handler = Handler(handlerThread.looper)

    private var inputTensorHeight = DEFAULT_TENSOR_HEIGHT
    private var inputTensorWidth = DEFAULT_TENSOR_WIDTH

    private var outputNumClasses = DEFAULT_NUM_CLASSES

    // Pre-allocated buffers — created once after interpreter init.
    private var inputTensorBuffer: TensorBuffer? = null
    private var outputTensorBuffer: TensorBuffer? = null
    private var chwArray: FloatArray? = null
    private var channelBuf: FloatArray? = null

    init {
        handler.post { initializeInterpreter() }
    }

    private fun initializeInterpreter() {
        synchronized(classifierLock) {
            if (classifier != null || isClosed) return

            try {
                val model = FileUtil.loadMappedFile(context, filePath)
                val options = Interpreter.Options().apply {
                    useNNAPI = false
                    useXNNPACK = true
                    // 3 classifiers run concurrently; 1 thread each avoids CPU saturation
                    // and thermal throttling on efficiency-core devices.
                    numThreads = 1
                }

                classifier = Interpreter(model, options)

                classifier?.let {
                    inputTensorHeight = it.getInputTensor(0).shape()[2]
                    inputTensorWidth = it.getInputTensor(0).shape()[3]

                    outputNumClasses = it.getOutputTensor(0).shape()[1]

                    val pixelCount = inputTensorHeight * inputTensorWidth
                    inputTensorBuffer = TensorBuffer.createFixedSize(
                        intArrayOf(1, 3, inputTensorHeight, inputTensorWidth), DataType.FLOAT32
                    )
                    outputTensorBuffer = TensorBuffer.createFixedSize(
                        intArrayOf(1, outputNumClasses), DataType.FLOAT32
                    )
                    chwArray = FloatArray(3 * pixelCount)
                    channelBuf = FloatArray(pixelCount)
                }

                Log.d(TAG, "TFLite interpreter initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize TFLite interpreter: ${e.message}")
            }
        }
    }

    private fun isReady(): Boolean = synchronized(classifierLock) {
        !isClosed && classifier != null
    }

    override fun getInputTensorShape(): Pair<Int, Int> = inputTensorHeight to inputTensorWidth

    override fun getOutputTensorShape(): Int = outputNumClasses

    override suspend fun classify(croppedBitmap: Bitmap): ClassifierResult? {
        if (!isReady()) return null

        return suspendCoroutine { continuation ->
            handler.post {
                val localInputTensor = inputTensorBuffer
                val localOutputTensor = outputTensorBuffer
                val localChwArray = chwArray
                val localChannelBuf = channelBuf
                if (localInputTensor == null || localOutputTensor == null ||
                    localChwArray == null || localChannelBuf == null
                ) {
                    continuation.resume(null)
                    return@post
                }

                try {
                    val startTime = System.currentTimeMillis()
                    val inputMatrix = prepareInputMatrix(croppedBitmap)
                    val preprocessedMatrix = preprocessMatrix(inputMatrix)
                    inputMatrix.release()

                    // Split channels natively then pack into CHW using pre-allocated arrays.
                    val channelMats = ArrayList<Mat>()
                    Core.split(preprocessedMatrix, channelMats)
                    preprocessedMatrix.release()
                    val channelSize = inputTensorHeight * inputTensorWidth
                    for (c in channelMats.indices) {
                        channelMats[c].get(0, 0, localChannelBuf)
                        localChannelBuf.copyInto(localChwArray, destinationOffset = c * channelSize)
                        channelMats[c].release()
                    }
                    localInputTensor.loadArray(localChwArray)

                    val logits = synchronized(classifierLock) {
                        if (!isReady()) return@post continuation.resume(null)
                        classifier?.run(localInputTensor.buffer, localOutputTensor.buffer)
                        localOutputTensor.floatArray.toList()
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

    override fun close() {
        synchronized(classifierLock) {
            if (isClosed) return
            isClosed = true

            handler.post {
                try {
                    classifier?.close()
                    classifier = null
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
        private const val DEFAULT_TENSOR_HEIGHT = 512
        private const val DEFAULT_TENSOR_WIDTH = 512
        private const val DEFAULT_NUM_CLASSES = 1

        private const val PIXEL_NORMALIZATION_SCALE = 1f / 255f
        private val NORMALIZE_MEAN = Scalar(0.485, 0.456, 0.406)
        private val NORMALIZE_STDDEV = Scalar(0.229, 0.224, 0.225)
    }
}