package com.vci.vectorcamapp.imaging.domain

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.model.results.ClassifierResult

interface SpecimenClassifier : WarmableModel {
    fun getInputTensorShape() : Pair<Int, Int>
    fun getOutputTensorShape() : Int
    suspend fun classify(croppedBitmap: Bitmap): ClassifierResult?
}
