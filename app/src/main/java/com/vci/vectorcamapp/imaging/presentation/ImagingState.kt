package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.model.Specimen

data class ImagingState(
    val currentImage: Bitmap? = null,
    val specimens: List<Specimen> = emptyList(),
)
