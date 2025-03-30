package com.vci.vectorcamapp.imaging.domain

import android.graphics.Bitmap

interface SpecimenDetector : AutoCloseable {
    fun detect(bitmap: Bitmap) : Detection?
}