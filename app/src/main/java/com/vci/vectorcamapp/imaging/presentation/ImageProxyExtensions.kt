package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

fun ImageProxy.toUprightBitmap() : Bitmap {
    val rotationDegrees = this.imageInfo.rotationDegrees.toFloat()

    val matrix = Matrix().apply {
        postRotate(rotationDegrees)
    }

    return Bitmap.createBitmap(
        this.toBitmap(),
        0,
        0,
        this.width,
        this.height,
        matrix,
        true,
    )
}