package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.vci.vectorcamapp.imaging.presentation.extensions.toUprightBitmap

class SpecimenImageAnalyzer(
    private val onFrameReady: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {
    companion object {
        const val FRAME_SKIP_RATE = 15
    }

    private var frameCounter = 0

    override fun analyze(frame: ImageProxy) {
        try {
            if (frameCounter % FRAME_SKIP_RATE == 0) {
                val bmp = frame.toUprightBitmap()
                onFrameReady(bmp)
            }
        } finally {
            frame.close()
            frameCounter++
        }
    }
}
