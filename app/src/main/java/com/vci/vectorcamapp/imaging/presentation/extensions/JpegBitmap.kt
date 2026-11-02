package com.vci.vectorcamapp.imaging.presentation.extensions

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

internal fun Bitmap.toReencodedJpeg(): Pair<ByteArray, Bitmap> {
    val jpegStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 100, jpegStream)
    val jpegBytes = jpegStream.toByteArray()

    val encoded = Mat(1, jpegBytes.size, CvType.CV_8UC1)
    encoded.put(0, 0, jpegBytes)
    val bgrMatrix = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_COLOR)
    encoded.release()
    val rgbaMatrix = Mat()
    Imgproc.cvtColor(bgrMatrix, rgbaMatrix, Imgproc.COLOR_BGR2RGBA)
    val decoded = createBitmap(rgbaMatrix.cols(), rgbaMatrix.rows())
    matToBitmap(rgbaMatrix, decoded)
    bgrMatrix.release()
    rgbaMatrix.release()
    return jpegBytes to decoded
}
