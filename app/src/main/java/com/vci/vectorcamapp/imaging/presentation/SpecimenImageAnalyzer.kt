package com.vci.vectorcamapp.imaging.presentation

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class SpecimenImageAnalyzer(
    private val onFrameReady: (ImageProxy) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameCounter = 0

    override fun analyze(frame: ImageProxy) {
        if (frameCounter % 30 == 0) {
            onFrameReady(frame)
            frameCounter = 1
        } else {
            frame.close()
            frameCounter++
        }
    }
}
