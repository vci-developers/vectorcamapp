package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Core
import org.opencv.core.Mat

fun ImageProxy.toUprightBitmap(): Bitmap {
    val rawBitmap = this.toBitmap()

    val src = Mat()
    bitmapToMat(rawBitmap, src)
    rawBitmap.recycle()

    val dst = Mat()
    when (this.imageInfo.rotationDegrees) {
        90 -> Core.rotate(src, dst, Core.ROTATE_90_CLOCKWISE)
        180 -> Core.rotate(src, dst, Core.ROTATE_180)
        270 -> Core.rotate(src, dst, Core.ROTATE_90_COUNTERCLOCKWISE)
        else -> src.copyTo(dst)
    }
    src.release()

    val result = createBitmap(dst.cols(), dst.rows())
    matToBitmap(dst, result)
    dst.release()

    return result
}
