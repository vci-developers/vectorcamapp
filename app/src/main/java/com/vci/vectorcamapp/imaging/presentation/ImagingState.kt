package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.imaging.domain.Detection

data class ImagingState(
    val currentSpecimenId: String = "",
    val currentImage: Bitmap? = null,
    val detection: Detection? = null,
    val specimens: List<Specimen> = emptyList(),
)
