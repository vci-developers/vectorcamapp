package com.vci.vectorcamapp.imaging.presentation.model.composites

import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi

data class SpecimenAndBoundingBoxUi(
    val specimen: Specimen,
    val boundingBoxUi: BoundingBoxUi
)
