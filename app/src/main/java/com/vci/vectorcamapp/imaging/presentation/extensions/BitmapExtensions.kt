package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.vci.vectorcamapp.imaging.domain.BoundingBox

fun Bitmap.resizeTo(width: Int, height: Int) : Bitmap {
    return Bitmap.createScaledBitmap(this, width, height, false)
}

fun Bitmap.cropToBoundingBoxAndPad(boundingBox: BoundingBox) : Bitmap {
    val x = (boundingBox.topLeftX * this.width).toInt().coerceIn(0, this.width - 1)
    val y = (boundingBox.topLeftY * this.height).toInt().coerceIn(0, this.height - 1)
    val width = (boundingBox.width * this.width).toInt().coerceAtMost(this.width - x)
    val height = (boundingBox.height * this.height).toInt().coerceAtMost(this.height - y)

    val cropped = Bitmap.createBitmap(this, x, y, width, height)

    val size = maxOf(width, height)
    val paddedBitmap = Bitmap.createBitmap(size, size, this.config ?: Bitmap.Config.ARGB_8888)

    val canvas = Canvas(paddedBitmap)

    canvas.drawColor(Color.BLACK)

    val offsetX = (size - width) / 2
    val offsetY = (size - height) / 2
    canvas.drawBitmap(cropped, offsetX.toFloat(), offsetY.toFloat(), null)

    return paddedBitmap
}