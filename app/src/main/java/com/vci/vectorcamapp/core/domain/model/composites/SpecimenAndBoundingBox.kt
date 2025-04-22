package com.vci.vectorcamapp.core.domain.model.composites

import com.vci.vectorcamapp.core.domain.model.BoundingBox
import com.vci.vectorcamapp.core.domain.model.Specimen

data class SpecimenAndBoundingBox(
    val specimen: Specimen,
    val boundingBox: BoundingBox
)