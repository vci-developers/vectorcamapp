package com.vci.vectorcamapp.imaging.presentation

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.util.Log

class SpecimenImageAnalyzer(
    private val onFrameReady: (ImageProxy) -> Unit
) : ImageAnalysis.Analyzer {
    
    companion object {
        // Reduced frame skip rate for better responsiveness with GPU acceleration
        const val FRAME_SKIP_RATE = 8 // Reduced from 15 to 8 for smoother detection
        private const val TAG = "SpecimenImageAnalyzer"
    }

    private var frameCounter = 0
    private var lastProcessingTime = System.currentTimeMillis()

    override fun analyze(frame: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Adaptive frame rate based on processing time
        val timeSinceLastFrame = currentTime - lastProcessingTime
        val shouldProcess = if (timeSinceLastFrame > 100) { // Force processing if >100ms since last
            true
        } else {
            frameCounter % FRAME_SKIP_RATE == 0
        }
        
        if (shouldProcess) {
            lastProcessingTime = currentTime
            frameCounter = 0
            onFrameReady(frame)
        } else {
            frame.close()
            frameCounter++
        }
    }
}
