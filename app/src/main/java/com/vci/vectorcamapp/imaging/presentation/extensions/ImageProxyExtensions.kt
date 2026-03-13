package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Core
import org.opencv.core.Mat
import androidx.core.graphics.createBitmap
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs

fun ImageProxy.toUprightBitmap(): Bitmap {
    val rawBitmap = this.toBitmap()

    val src = Mat()
    bitmapToMat(rawBitmap, src)
    rawBitmap.recycle()

    val dst = Mat()
    Core.rotate(src, dst, Core.ROTATE_90_CLOCKWISE)
    src.release()

    val result = createBitmap(dst.cols(), dst.rows())
    matToBitmap(dst, result)
    dst.release()

    return result
}

fun ImageProxy.toUprightMat(): Mat {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    return Imgcodecs.imdecode(MatOfByte(*bytes), Imgcodecs.IMREAD_COLOR)
}
