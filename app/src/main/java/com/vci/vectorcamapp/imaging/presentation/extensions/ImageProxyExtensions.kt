package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

/**
 * Returns a new bitmap rotated by [rotationDegrees] (0, 90, 180, 270), or this bitmap if 0.
 * The source bitmap is recycled when a new rotated bitmap is created.
 */
fun Bitmap.rotateBy(rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return this
    val srcWidth = width
    val srcHeight = height
    val (outWidth, outHeight) = if (rotationDegrees == 90 || rotationDegrees == 270) {
        srcHeight to srcWidth
    } else {
        srcWidth to srcHeight
    }
    val output = Bitmap.createBitmap(outWidth, outHeight, config ?: Bitmap.Config.ARGB_8888)
    Canvas(output).apply {
        // 1. Place output center at origin for rotation pivot
        translate(outWidth / 2f, outHeight / 2f)
        rotate(rotationDegrees.toFloat())
        // 2. Offset so source bitmap center is at pivot (use source dimensions, not output)
        translate(-srcWidth / 2f, -srcHeight / 2f)
        drawBitmap(this@rotateBy, 0f, 0f, null)
    }
    recycle()
    return output
}

/**
 * Returns a new bitmap that is:
 * - center–cropped to a 4:3 aspect ratio (same as Camera2 preview), and
 * - rotated to match the given [sensorOrientation].
 *
 * This roughly matches the behavior of the old CameraX-based toUprightBitmap().
 */
fun Bitmap.toUprightBitmap(sensorOrientation: Int): Bitmap {
    val source = this
    val aspect = 4f / 3f
    val srcWidth = source.width
    val srcHeight = source.height

    // Center-crop to 4:3 so we match the preview / detector aspect
    val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
    val cropRect: Rect = if (srcAspect > aspect) {
        // Too wide: trim left/right
        val targetWidth = (srcHeight * aspect).toInt()
        val left = (srcWidth - targetWidth) / 2
        Rect(left, 0, left + targetWidth, srcHeight)
    } else {
        // Too tall: trim top/bottom
        val targetHeight = (srcWidth / aspect).toInt()
        val top = (srcHeight - targetHeight) / 2
        Rect(0, top, srcWidth, top + targetHeight)
    }

    val cropped = Bitmap.createBitmap(
        source,
        cropRect.left,
        cropRect.top,
        cropRect.width(),
        cropRect.height()
    )

    // Rotate into upright orientation.
    // We intentionally do not recycle the original source here because
    // it may be owned by Camera2Controller; the caller can decide lifetime.
    return cropped.rotateBy(sensorOrientation)
}
