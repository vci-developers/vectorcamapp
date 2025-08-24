package com.vci.vectorcamapp.complete_session.domain.model

import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage

data class SpecimenImageItem(
    val specimen: Specimen,
    val specimenImage: SpecimenImage,
    val badgeText: String
)
