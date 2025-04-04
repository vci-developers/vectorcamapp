package com.vci.vectorcamapp.imaging.domain

import android.graphics.Bitmap

interface SpeciesClassifier : AutoCloseable {
    suspend fun classifySpecies(bitmap: Bitmap): Int?
    fun getInputTensorShape() : Pair<Int, Int>
    fun getOutputTensorShape() : Int
}