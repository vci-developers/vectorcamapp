package com.vci.vectorcamapp.imaging.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.vci.vectorcamapp.imaging.domain.SpecimenClassifier
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    private val imageProcessor by lazy {
        ImageProcessor.Builder()
            .add(CastOp(DataType.FLOAT32))
            .add(NormalizeOp(0f, 255f))
            .add(NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD))
            .build()
    }

    private var inputNumChannels = DEFAULT_NUM_CHANNELS
    private var inputTensorHeight = DEFAULT_TENSOR_HEIGHT
    private var inputTensorWidth = DEFAULT_TENSOR_WIDTH

    private var outputNumClasses = DEFAULT_NUM_CLASSES

    init {
        handler.post { initializeInterpreter() }
    }

    private fun initializeInterpreter() {
        synchronized(classifierLock) {
            if (classifier != null || isClosed) return

            try {
                val model = FileUtil.loadMappedFile(context, filePath)
                val options = Interpreter.Options()

                options.setNumThreads(Runtime.getRuntime().availableProcessors())
                classifier = Interpreter(model, options)

                classifier?.let {
                    inputNumChannels = it.getInputTensor(0).shape()[1]
                    inputTensorHeight = it.getInputTensor(0).shape()[2]
                    inputTensorWidth = it.getInputTensor(0).shape()[3]

                    outputNumClasses = it.getOutputTensor(0).shape()[1]
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

    override suspend fun classify(bitmap: Bitmap): Int? {
        if (!isReady()) return null

        return suspendCoroutine { continuation ->
            handler.post {
                try {
                    Log.d(TAG, "Inference started!")
                    val tensorImage = TensorImage(DataType.FLOAT32).apply {
                        load(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                    }
                    val processedImage = imageProcessor.process(tensorImage)
                    val inputBufferHWC = processedImage.buffer.asFloatBuffer()

                    val chwArray = FloatArray(inputNumChannels * inputTensorHeight * inputTensorWidth)
                    for (y in 0 until inputTensorHeight) {
                        for (x in 0 until inputTensorWidth) {
                            for (c in 0 until inputNumChannels) {
                                val hwcIndex = (y * inputTensorWidth + x) * inputNumChannels + c
                                val chwIndex = c * inputTensorHeight * inputTensorWidth + y * inputTensorWidth + x
                                chwArray[chwIndex] = inputBufferHWC.get(hwcIndex)
                            }
                        }
                    }

                    val input = TensorBuffer.createFixedSize(
                        intArrayOf(
                            1,
                            inputNumChannels,
                            inputTensorHeight,
                            inputTensorWidth
                        ), DataType.FLOAT32
                    )
                    input.loadArray(chwArray)

                    val output = TensorBuffer.createFixedSize(
                        intArrayOf(1, outputNumClasses), DataType.FLOAT32
                    )

                    val result = synchronized(classifierLock) {
                        if (!isReady()) return@post continuation.resume(null)
                        classifier?.run(input.buffer, output.buffer)
                        getClass(output.floatArray)
                    }

                    Log.d(TAG, "Inference result: $result")
                    continuation.resume(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Inference failed: ${e.message}")
                    continuation.resume(null)
                }
            }
        }
    }

    private fun getClass(logits: FloatArray): Int {
        if (logits.isEmpty()) return -1
        return logits.indices.maxByOrNull { logits[it] } ?: -1
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
        private const val DEFAULT_NUM_CHANNELS = 3
        private const val DEFAULT_TENSOR_HEIGHT = 300
        private const val DEFAULT_TENSOR_WIDTH = 300
        private const val DEFAULT_NUM_CLASSES = 1

        private val NORMALIZE_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val NORMALIZE_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}