package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

/**
 * Converts this ImageProxy (YUV_420_888) to ARGB Bitmap without going through JPEG.
 * Uses direct YUV→ARGB conversion with proper plane strides.
 * Used as CPU fallback when RenderScript is unavailable.
 */
fun ImageProxy.imageProxyToBitmapCpu(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    val yRowStride = planes[0].rowStride
    val yPixelStride = planes[0].pixelStride
    val uRowStride = planes[1].rowStride
    val uPixelStride = planes[1].pixelStride
    val vRowStride = planes[2].rowStride
    val vPixelStride = planes[2].pixelStride

    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val yArray = ByteArray(ySize).also { yBuffer.get(it); yBuffer.rewind() }
    val uArray = ByteArray(uSize).also { uBuffer.get(it); uBuffer.rewind() }
    val vArray = ByteArray(vSize).also { vBuffer.get(it); vBuffer.rewind() }

    var pixIdx = 0
    for (j in 0 until height) {
        for (i in 0 until width) {
            val yIdx = j * yRowStride + i * yPixelStride
            val uvRow = j / 2
            val uvCol = i / 2
            val uIdx = uvRow * uRowStride + uvCol * uPixelStride
            val vIdx = uvRow * vRowStride + uvCol * vPixelStride
            if (yIdx >= ySize || uIdx >= uSize || vIdx >= vSize) {
                pixels[pixIdx++] = 0xff shl 24
                continue
            }
            val y = yArray[yIdx].toInt() and 0xff
            val u = (uArray[uIdx].toInt() and 0xff) - 128
            val v = (vArray[vIdx].toInt() and 0xff) - 128

            val r = (y + (v * 1436) / 1024).coerceIn(0, 255)
            val g = (y - (u * 352 - v * 731) / 1024).coerceIn(0, 255)
            val b = (y + (u * 1814) / 1024).coerceIn(0, 255)

            pixels[pixIdx++] = 0xff shl 24 or (r shl 16) or (g shl 8) or b
        }
    }

    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return out
}

/**
 * Converts this ImageProxy to an upright bitmap using [imageInfo.rotationDegrees].
 * Prefer [YuvToRgbConverter.convertToUprightBitmap] when available for GPU acceleration.
 */
fun ImageProxy.toUprightBitmap(): Bitmap {
    val bitmap = this.imageProxyToBitmapCpu()
    val rotationDegrees = imageInfo.rotationDegrees
    if (rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply {
        postRotate(rotationDegrees.toFloat(), bitmap.width / 2f, bitmap.height / 2f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
