package com.vci.vectorcamapp.imaging.domain.repository

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.core.domain.model.InferenceResult
import com.vci.vectorcamapp.core.domain.model.results.ClassifierResult
import com.vci.vectorcamapp.core.domain.model.results.DetectorResult

interface InferenceRepository {
    suspend fun readSpecimenId(bitmap: Bitmap): String
    suspend fun detectSpecimen(bitmap: Bitmap): List<DetectorResult>
    suspend fun classifySpecimen(croppedBitmap: Bitmap): Triple<ClassifierResult?, ClassifierResult?, ClassifierResult?>
    suspend fun computeAutofocusCentroid(bitmap: Bitmap, detection: InferenceResult): Offset?
    fun closeResources()
}
