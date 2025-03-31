package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import com.vci.vectorcamapp.imaging.domain.BoundingBox

fun Bitmap.resizeTo(width: Int, height: Int) : Bitmap {
    return Bitmap.createScaledBitmap(this, width, height, false)
}

fun Bitmap.cropToBoundingBox(boundingBox: BoundingBox) : Bitmap {
    val x = (boundingBox.topLeftX * this.width).toInt().coerceIn(0, this.width - 1)
    val y = (boundingBox.topLeftY * this.height).toInt().coerceIn(0, this.height - 1)
    val width = (boundingBox.width * this.width).toInt().coerceAtMost(this.width - x)
    val height = (boundingBox.height * this.height).toInt().coerceAtMost(this.height - y)
    return Bitmap.createBitmap(this, x, y, width, height)
}