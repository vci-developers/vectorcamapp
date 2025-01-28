package com.vci.vectorcamapp.imaging.presentation

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.vci.vectorcamapp.imaging.domain.Detection
import com.vci.vectorcamapp.imaging.domain.SpecimenDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpecimenImageAnalyzer(
    private val detector: SpecimenDetector,
    private val onResult: (Detection?) -> Unit
) : ImageAnalysis.Analyzer {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun analyze(image: ImageProxy) {
       scope.launch {
            try {
                val detection = detector.detect(image.toUprightBitmap())
                onResult(detection)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                image.close()
            }
       }
    }

    fun close() {
        if (detector is AutoCloseable) {
            detector.close()
        }
        scope.cancel()
    }
}