package com.vci.vectorcamapp.imaging.domain

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.model.BoundingBox
import java.io.Closeable

interface SpecimenDetector : Closeable {
    suspend fun detect(bitmap: Bitmap): BoundingBox?
    fun getInputTensorShape(): Pair<Int, Int>
    fun getOutputTensorShape(): Pair<Int, Int>
}
