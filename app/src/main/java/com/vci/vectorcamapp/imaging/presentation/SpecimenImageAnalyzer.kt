package com.vci.vectorcamapp.imaging.presentation

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicBoolean

class SpecimenImageAnalyzer(
    private val onFrameReady: (ImageProxy) -> Unit
) : ImageAnalysis.Analyzer {
    
    companion object {
        // Increased from 15 to 30 for better performance - processes every 30th frame
        const val FRAME_SKIP_RATE = 30
    }

    private var frameCounter = 0
    private val isProcessing = AtomicBoolean(false)

    override fun analyze(frame: ImageProxy) {
        // Skip frame if we're still processing the previous one
        if (isProcessing.get()) {
            frame.close()
            return
        }
        
        frameCounter++
        
        if (frameCounter % FRAME_SKIP_RATE == 0) {
            // Set processing flag to prevent concurrent processing
            if (isProcessing.compareAndSet(false, true)) {
                try {
                    // Create a wrapper that resets the processing flag when done
                    val wrappedFrame = ProcessingImageProxy(frame) {
                        isProcessing.set(false)
                    }
                    onFrameReady(wrappedFrame)
                } catch (e: Exception) {
                    // Reset flag if there's an error
                    isProcessing.set(false)
                    frame.close()
                }
            } else {
                // Another frame is being processed, skip this one
                frame.close()
            }
            frameCounter = 0
        } else {
            frame.close()
        }
    }
    
    /**
     * Wrapper class that automatically resets processing flag when closed
     */
    private class ProcessingImageProxy(
        private val delegate: ImageProxy,
        private val onClose: () -> Unit
    ) : ImageProxy by delegate {
        
        private val isClosed = AtomicBoolean(false)
        
        override fun close() {
            if (isClosed.compareAndSet(false, true)) {
                try {
                    delegate.close()
                } finally {
                    onClose()
                }
            }
        }
    }
}
