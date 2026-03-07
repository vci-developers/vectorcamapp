package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
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
    Core.rotate(src, dst, Core.ROTATE_90_CLOCKWISE)
    src.release()

    val result = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888)
    matToBitmap(dst, result)
    dst.release()

    return result
}
