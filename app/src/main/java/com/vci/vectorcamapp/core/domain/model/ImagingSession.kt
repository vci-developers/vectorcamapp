package com.vci.vectorcamapp.core.domain.model

import java.util.Date

data class ImagingSession(
    val id: String,
    val createdAt: Date,
    val submittedAt: Date,
    val surveillanceForm: SurveillanceForm,
    val specimens: List<Specimen>,
)
