package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import android.graphics.Canvas

/**
 * Returns a new bitmap rotated by [rotationDegrees] (0, 90, 180, 270), or this bitmap if 0.
 * The source bitmap is recycled when a new rotated bitmap is created.
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
