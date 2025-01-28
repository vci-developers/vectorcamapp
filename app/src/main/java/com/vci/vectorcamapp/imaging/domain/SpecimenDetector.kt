package com.vci.vectorcamapp.imaging.domain

import android.graphics.Bitmap

interface SpecimenDetector {
    fun detect(bitmap: Bitmap) : Detection?
}