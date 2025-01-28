package com.vci.vectorcamapp.imaging.data

import android.content.Context
import android.graphics.Bitmap
import com.vci.vectorcamapp.imaging.domain.BoundingBox
import com.vci.vectorcamapp.imaging.domain.Detection
import com.vci.vectorcamapp.imaging.domain.SpecimenDetector
import com.vci.vectorcamapp.imaging.presentation.toDetection
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class TfLiteSpecimenDetector(
    private val context: Context
) : SpecimenDetector {

    private var detector: Interpreter? = null

    private fun setupDetector() {
        val model = FileUtil.loadMappedFile(context, "detect.tflite")
        val options = Interpreter.Options().setNumThreads(4)
        detector = Interpreter(model, options)
    }

    override fun detect(bitmap: Bitmap): Detection? {
        if (detector == null) {
            setupDetector()
        }

        val inputShape = detector?.getInputTensor(0)?.shape()
        val outputShape = detector?.getOutputTensor(0)?.shape()

        val tensorWidth = inputShape?.getOrNull(1) ?: DEFAULT_TENSOR_WIDTH
        val tensorHeight = inputShape?.getOrNull(2) ?: DEFAULT_TENSOR_HEIGHT
        val numChannels = outputShape?.getOrNull(1) ?: DEFAULT_NUM_CHANNELS
        val numElements = outputShape?.getOrNull(2) ?: DEFAULT_NUM_ELEMENTS

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidth, tensorHeight, false)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)

        val imageProcessor =
            ImageProcessor.Builder().add(CastOp(DataType.FLOAT32)).add(NormalizeOp(0f, 255f))
                .build()

        val input = imageProcessor.process(tensorImage)
        val output =
            TensorBuffer.createFixedSize(intArrayOf(1, numChannels, numElements), DataType.FLOAT32)

        detector?.run(input.buffer, output.buffer)
        return getBestBox(output.floatArray, numChannels, numElements)?.toDetection(
            tensorWidth, tensorHeight, bitmap.width, bitmap.height
        )
    }

    private fun getBestBox(boxes: FloatArray, numChannels: Int, numElements: Int): BoundingBox? {
        val boundingBoxes = mutableListOf<BoundingBox>()

        for (i in 0 until numChannels) {
            val baseIndex = i * numElements
            val confidence = boxes[baseIndex + 4]

            if (confidence < CONFIDENCE_THRESHOLD) continue

            var maxClassScore = -1.0f
            var bestClassIndex = -1
            for (j in 5 until numElements) {
                if (boxes[baseIndex + j] > maxClassScore) {
                    maxClassScore = boxes[baseIndex + j]
                    bestClassIndex = j - 5
                }
            }

            val centerX = boxes[baseIndex]
            val centerY = boxes[baseIndex + 1]
            val width = boxes[baseIndex + 2]
            val height = boxes[baseIndex + 3]

            val topLeftX = centerX - (width / 2f)
            val topLeftY = centerY - (height / 2f)
            val bottomRightX = centerX + (width / 2f)
            val bottomRightY = centerY + (height / 2f)

            if (topLeftX < 0f || topLeftX > 1f || topLeftY < 0f || topLeftY > 1f || bottomRightX < 0f || bottomRightX > 1f || bottomRightY < 0f || bottomRightY > 1f) {
                continue
            }

            boundingBoxes.add(
                BoundingBox(
                    topLeftX = topLeftX,
                    topLeftY = topLeftY,
                    bottomRightX = bottomRightX,
                    bottomRightY = bottomRightY,
                    centerX = centerX,
                    centerY = centerY,
                    width = width,
                    height = height,
                    confidence = confidence,
                    classId = bestClassIndex
                )
            )
        }

        if (boundingBoxes.isEmpty()) return null

        // Apply Non-Maximum Suppression (NMS) to get the best bounding box
        return applyNMS(boundingBoxes).maxByOrNull { it.confidence }
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.confidence }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val bestBox = sortedBoxes.removeAt(0)
            selectedBoxes.add(bestBox)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val currentBox = iterator.next()
                val iou = calculateIoU(bestBox, currentBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val topLeftX = maxOf(box1.topLeftX, box2.topLeftX)
        val topLeftY = maxOf(box1.topLeftY, box2.topLeftY)
        val bottomRightX = minOf(box1.bottomRightX, box2.bottomRightX)
        val bottomRightY = minOf(box1.bottomRightY, box2.bottomRightY)

        val intersectionWidth = maxOf(0f, bottomRightX - topLeftX)
        val intersectionHeight = maxOf(0f, bottomRightY - topLeftY)
        val intersectionArea = intersectionWidth * intersectionHeight

        val box1Area = box1.width * box1.height
        val box2Area = box2.width * box2.height

        return if (box1Area + box2Area - intersectionArea > 0f) {
            intersectionArea / (box1Area + box2Area - intersectionArea)
        } else {
            0f
        }
    }

    companion object {
        private const val DEFAULT_TENSOR_WIDTH = 640
        private const val DEFAULT_TENSOR_HEIGHT = 640
        private const val DEFAULT_NUM_CHANNELS = 25200
        private const val DEFAULT_NUM_ELEMENTS = 6
        private const val CONFIDENCE_THRESHOLD = 0.6f
        private const val IOU_THRESHOLD = 0.7f
    }
}