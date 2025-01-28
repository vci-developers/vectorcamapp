package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.imaging.domain.Detection

data class ImagingState(
    val detection: Detection? = null,
    val currentImage: Bitmap? = null,
    val specimens: List<Specimen> = emptyList(),
)
