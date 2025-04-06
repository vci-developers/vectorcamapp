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
    private var initialized = false
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

    init {
        handler.post { initializeInterpreter() }
    }

    private fun initializeInterpreter() {
        synchronized(classifierLock) {
            if (initialized || isClosed) return

            try {
                val model = FileUtil.loadMappedFile(context, filePath)
                val options = Interpreter.Options()

                options.setNumThreads(Runtime.getRuntime().availableProcessors())
                classifier = Interpreter(model, options)
                initialized = true
                Log.d(TAG, "TFLite interpreter initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize TFLite interpreter: ${e.message}")
            }
        }
    }

    private fun isReady(): Boolean = synchronized(classifierLock) {
        initialized && !isClosed && classifier != null
    }

    private fun getNumChannels(): Int {
        synchronized(classifierLock) {
            if (!isReady()) {
                Log.w(TAG, "Classifier not ready in getNumChannels")
                return DEFAULT_NUM_CHANNELS
            }

            return try {
                val shape = classifier!!.getInputTensor(0).shape()
                shape[1]
            } catch (e: Exception) {
                Log.e(TAG, "getNumChannels failed: ${e.message}")
                DEFAULT_NUM_CHANNELS
            }
        }
    }

    override fun getInputTensorShape(): Pair<Int, Int> {
        synchronized(classifierLock) {
            if (!isReady()) {
                Log.w(TAG, "Classifier not ready in getInputTensorShape")
                return DEFAULT_TENSOR_HEIGHT to DEFAULT_TENSOR_WIDTH
            }

            return try {
                val shape = classifier!!.getInputTensor(0).shape()
                shape[2] to shape[3]
            } catch (e: Exception) {
                Log.e(TAG, "getInputTensorShape failed: ${e.message}")
                DEFAULT_TENSOR_HEIGHT to DEFAULT_TENSOR_WIDTH
            }
        }
    }

    override fun getOutputTensorShape(): Int {
        synchronized(classifierLock) {
            if (!isReady()) {
                Log.w(TAG, "Classifier not ready in getOutputTensorShape")
                return DEFAULT_NUM_CLASSES
            }

            return try {
                val shape = classifier!!.getOutputTensor(0).shape()
                shape[1]
            } catch (e: Exception) {
                Log.e(TAG, "getOutputTensorShape failed: ${e.message}")
                DEFAULT_NUM_CLASSES
            }
        }
    }

    override suspend fun classify(bitmap: Bitmap): Int? {
        if (!isReady()) return null

        return suspendCoroutine { continuation ->
            handler.post {
                try {
                    Log.d(TAG, "Inference started!")
                    val numChannels = getNumChannels()
                    val (tensorHeight, tensorWidth) = getInputTensorShape()
                    val numClasses = getOutputTensorShape()
                    val tensorImage = TensorImage(DataType.FLOAT32).apply {
                        load(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                    }
                    val processedImage = imageProcessor.process(tensorImage)
                    val inputBufferHWC = processedImage.buffer.asFloatBuffer()

                    val chwArray = FloatArray(numChannels * tensorHeight * tensorWidth)
                    for (y in 0 until tensorHeight) {
                        for (x in 0 until tensorWidth) {
                            for (c in 0 until numChannels) {
                                val hwcIndex = (y * tensorWidth + x) * numChannels + c
                                val chwIndex = c * tensorHeight * tensorWidth + y * tensorWidth + x
                                chwArray[chwIndex] = inputBufferHWC.get(hwcIndex)
                            }
                        }
                    }

                    val input = TensorBuffer.createFixedSize(
                        intArrayOf(
                            1,
                            numChannels,
                            tensorHeight,
                            tensorWidth
                        ), DataType.FLOAT32
                    )
                    input.loadArray(chwArray)

                    val output = TensorBuffer.createFixedSize(
                        intArrayOf(1, numClasses), DataType.FLOAT32
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
        private const val DEFAULT_TENSOR_HEIGHT = 300
        private const val DEFAULT_TENSOR_WIDTH = 300
        private const val DEFAULT_NUM_CLASSES = 7
        private const val DEFAULT_NUM_CHANNELS = 3

        private val NORMALIZE_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val NORMALIZE_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}