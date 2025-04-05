package com.vci.vectorcamapp.imaging.domain

import android.graphics.Bitmap
import java.io.Closeable

interface SpecimenClassifier : Closeable {
    suspend fun classify(bitmap: Bitmap): Int?
    fun getInputTensorShape() : Pair<Int, Int>
    fun getOutputTensorShape() : Int
}
