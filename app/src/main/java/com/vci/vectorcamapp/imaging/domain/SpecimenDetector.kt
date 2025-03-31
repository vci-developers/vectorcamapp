package com.vci.vectorcamapp.imaging.domain

import android.graphics.Bitmap

interface SpecimenDetector : AutoCloseable {
    fun detect(bitmap: Bitmap) : BoundingBox?
    fun getInputTensorShape() : Pair<Int, Int>
    fun getOutputTensorShape() : Pair<Int, Int>
}