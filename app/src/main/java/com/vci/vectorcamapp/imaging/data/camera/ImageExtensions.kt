package com.vci.vectorcamapp.imaging.data.camera

import android.graphics.Bitmap
import android.media.Image
import androidx.core.graphics.createBitmap
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

private const val ROTATION_90_DEGREES = 90
private const val ROTATION_180_DEGREES = 180
private const val ROTATION_270_DEGREES = 270

private const val CHROMA_DIMENSION_DIVISOR = 2

fun Image.toUprightBitmap(rotationDegrees: Int): Bitmap {
    val nv21 = toNv21ByteArray()

    val yuvMat = Mat(height + height / CHROMA_DIMENSION_DIVISOR, width, CvType.CV_8UC1)
    yuvMat.put(0, 0, nv21)

    val rgbaMat = Mat()
    Imgproc.cvtColor(yuvMat, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21)
    yuvMat.release()

    val rotatedMat = Mat()
    when (rotationDegrees) {
        ROTATION_90_DEGREES -> Core.rotate(rgbaMat, rotatedMat, Core.ROTATE_90_CLOCKWISE)
        ROTATION_180_DEGREES -> Core.rotate(rgbaMat, rotatedMat, Core.ROTATE_180)
        ROTATION_270_DEGREES -> Core.rotate(rgbaMat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
        else -> rgbaMat.copyTo(rotatedMat)
    }
    rgbaMat.release()

    val bitmap = createBitmap(rotatedMat.cols(), rotatedMat.rows())
    matToBitmap(rotatedMat, bitmap)
    rotatedMat.release()

    return bitmap
}

private fun Image.toNv21ByteArray(): ByteArray {
    val ySize = width * height
    val uvSize = ySize / CHROMA_DIMENSION_DIVISOR
    val nv21 = ByteArray(ySize + uvSize)

    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer = yPlane.buffer
    val yRowStride = yPlane.rowStride
    if (yRowStride == width) {
        yBuffer.get(nv21, 0, ySize)
    } else {
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            yBuffer.get(nv21, row * width, width)
        }
    }

    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    val uvRowStride = uPlane.rowStride
    val uvPixelStride = uPlane.pixelStride
    val chromaWidth = width / CHROMA_DIMENSION_DIVISOR
    val chromaHeight = height / CHROMA_DIMENSION_DIVISOR

    var pos = ySize
    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {
            nv21[pos++] = vBuffer.get(row * uvRowStride + col * uvPixelStride)
            nv21[pos++] = uBuffer.get(row * uvRowStride + col * uvPixelStride)
        }
    }

    return nv21
}
