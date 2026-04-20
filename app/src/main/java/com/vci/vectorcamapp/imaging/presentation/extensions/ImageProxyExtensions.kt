package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Core
import org.opencv.core.Mat

fun ImageProxy.toUprightBitmap(): Bitmap {
    val rawBitmap = toBitmap()
    val rotationDegrees = ((imageInfo.rotationDegrees % 360) + 360) % 360

    if (rotationDegrees == 0) {
        return rawBitmap
    }

    val rotateCode = when (rotationDegrees) {
        90 -> Core.ROTATE_90_CLOCKWISE
        180 -> Core.ROTATE_180
        270 -> Core.ROTATE_90_COUNTERCLOCKWISE
        else -> null
    }

    if (rotateCode == null) {
        return rawBitmap
    }

    val src = Mat()
    bitmapToMat(rawBitmap, src)
    rawBitmap.recycle()

    val dst = Mat()
    Core.rotate(src, dst, rotateCode)
    src.release()

    val result = createBitmap(dst.cols(), dst.rows())
    matToBitmap(dst, result)
    dst.release()

    return result
}
