@file:Suppress("DEPRECATION")
package com.vci.vectorcamapp.imaging.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import androidx.camera.core.ImageProxy
import com.vci.vectorcamapp.imaging.presentation.extensions.imageProxyToBitmapCpu

/**
 * Converts ImageProxy (YUV_420_888) to upright Bitmap.
 * Uses RenderScript (GPU) when available; falls back to CPU conversion.
 */
class YuvToRgbConverter(context: Context) {

    private var renderScript: RenderScript? = null
    private var yuvToRgbScript: ScriptIntrinsicYuvToRGB? = null
    private var inputAllocation: Allocation? = null
    private var outputAllocation: Allocation? = null
    private var nv21Buffer: ByteArray? = null
    private var lastWidth = 0
    private var lastHeight = 0

    init {
        try {
            renderScript = RenderScript.create(context)
            yuvToRgbScript = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript))
        } catch (e: Exception) {
            renderScript = null
            yuvToRgbScript = null
        }
    }

    @Synchronized
    fun convertToUprightBitmap(image: ImageProxy): Bitmap {
        val bitmap = when (image.format) {
            ImageFormat.JPEG -> decodeJpegImageProxy(image)
            else -> try {
                if (renderScript != null && yuvToRgbScript != null) {
                    convertWithRenderScript(image)
                } else {
                    image.imageProxyToBitmapCpu()
                }
            } catch (e: Exception) {
                image.imageProxyToBitmapCpu()
            }
        }

        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat(), bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun convertWithRenderScript(image: ImageProxy): Bitmap {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21Size = ySize + uvSize

        if (nv21Buffer == null || nv21Buffer!!.size != nv21Size) {
            nv21Buffer = ByteArray(nv21Size)
        }
        val nv21 = nv21Buffer!!

        copyImageToNv21(image, nv21)

        if (inputAllocation == null || lastWidth != width || lastHeight != height) {
            inputAllocation?.destroy()
            outputAllocation?.destroy()
            val yuvType = Type.Builder(renderScript, Element.U8(renderScript))
                .setX(width)
                .setY(height)
                .setYuvFormat(ImageFormat.NV21)
            inputAllocation = Allocation.createTyped(renderScript, yuvType.create(), Allocation.USAGE_SCRIPT)
            val rgbaType = Type.Builder(renderScript, Element.RGBA_8888(renderScript))
                .setX(width)
                .setY(height)
            outputAllocation = Allocation.createTyped(renderScript, rgbaType.create(), Allocation.USAGE_SCRIPT)
            lastWidth = width
            lastHeight = height
        }

        inputAllocation!!.copyFrom(nv21)
        yuvToRgbScript!!.setInput(inputAllocation)
        yuvToRgbScript!!.forEach(outputAllocation)

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outputAllocation!!.copyTo(outputBitmap)
        return outputBitmap
    }

    private fun decodeJpegImageProxy(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val jpegBytes = ByteArray(buffer.remaining()).also { buffer.get(it); buffer.rewind() }
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: throw IllegalArgumentException("Failed to decode JPEG")
    }

    private fun copyImageToNv21(image: ImageProxy, nv21: ByteArray) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val width = image.width
        val height = image.height
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        val yArray = ByteArray(yPlane.buffer.remaining()).also { yPlane.buffer.get(it); yPlane.buffer.rewind() }
        val uArray = ByteArray(uPlane.buffer.remaining()).also { uPlane.buffer.get(it); uPlane.buffer.rewind() }
        val vArray = ByteArray(vPlane.buffer.remaining()).also { vPlane.buffer.get(it); vPlane.buffer.rewind() }

        var nv21Idx = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIdx = j * yRowStride + i * yPixelStride
                if (yIdx < yArray.size) nv21[nv21Idx++] = yArray[yIdx]
            }
        }

        for (j in 0 until height step 2) {
            for (i in 0 until width step 2) {
                val uvRow = j / 2
                val uvCol = i / 2
                val vIdx = uvRow * vRowStride + uvCol * vPixelStride
                val uIdx = uvRow * uRowStride + uvCol * uPixelStride
                if (vIdx < vArray.size && uIdx < uArray.size) {
                    nv21[nv21Idx++] = vArray[vIdx]
                    nv21[nv21Idx++] = uArray[uIdx]
                }
            }
        }
    }

    fun release() {
        inputAllocation?.destroy()
        outputAllocation?.destroy()
        yuvToRgbScript?.destroy()
        renderScript?.destroy()
        inputAllocation = null
        outputAllocation = null
        yuvToRgbScript = null
        renderScript = null
    }
}
