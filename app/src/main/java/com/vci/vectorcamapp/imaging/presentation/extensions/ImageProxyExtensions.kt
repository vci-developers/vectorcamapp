package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.camera.core.ImageProxy

/**
 * Converts this [ImageProxy] to an upright [Bitmap] by applying the correct rotation.
 * Uses [ImageProxy.getImageInfo].[rotationDegrees] when non-zero. For ImageCapture in-memory
 * capture, some devices report 0 even though the buffer is in sensor orientation; pass
 * [captureRotationDegrees] (display rotation at capture: 0, 90, 180, 270) to correct in that case.
 */
fun ImageProxy.toUprightBitmap(captureRotationDegrees: Int? = null): Bitmap {
    val bitmap = this.toBitmap()
    val rotation = resolveRotationDegrees(captureRotationDegrees)
    return bitmap.rotateBy(rotation)
}

/**
 * Rotation to apply for display (0, 90, 180, 270). For use when you already have a bitmap
 * and rotation from [ImageProxy.imageInfo.rotationDegrees] or capture display rotation.
 */
internal fun ImageProxy.resolveRotationDegrees(captureRotationDegrees: Int?): Int {
    var rotation = imageInfo.rotationDegrees
    if (rotation == 0 && captureRotationDegrees != null && captureRotationDegrees != 0) {
        rotation = captureRotationDegrees
    }
    return rotation
}

/**
 * Returns a new bitmap rotated by [rotationDegrees] (0, 90, 180, 270), or this bitmap if 0.
 * Caller must recycle the original if the return value is a new bitmap.
 */
fun Bitmap.rotateBy(rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return this
    val (outWidth, outHeight) = if (rotationDegrees == 90 || rotationDegrees == 270) {
        height to width
    } else {
        width to height
    }
    val output = Bitmap.createBitmap(outWidth, outHeight, config ?: Bitmap.Config.ARGB_8888)
    Canvas(output).apply {
        translate(outWidth / 2f, outHeight / 2f)
        rotate(rotationDegrees.toFloat())
        translate(-width / 2f, -height / 2f)
        drawBitmap(this@rotateBy, 0f, 0f, null)
    }
    recycle()
    return output
}
