package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi

data class ImagingState(
    val isCapturing: Boolean = false,
    val currentSpecimenId: String = "",
    val currentSpecies: String? = null,
    val currentSex: String? = null,
    val currentAbdomenStatus: String? = null,
    val currentImage: Bitmap? = null,
    val currentBoundingBoxUi: BoundingBoxUi? = null,
    val specimens: List<Specimen> = emptyList(),
)
