package com.vci.vectorcamapp.imaging.presentation

import android.graphics.Bitmap
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi

data class ImagingState(
    val currentSpecimenId: String = "",
    val currentSpecies: String = "",
    val currentSex: String = "",
    val currentAbdomenStatus: String = "",
    val currentImage: Bitmap? = null,
    val currentBoundingBoxUi: BoundingBoxUi? = null,
    val specimens: List<Specimen> = emptyList(),
)
