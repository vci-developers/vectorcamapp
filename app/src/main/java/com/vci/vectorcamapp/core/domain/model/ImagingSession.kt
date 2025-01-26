package com.vci.vectorcamapp.core.domain.model

data class ImagingSession(
    val id: String,
    val surveillanceForm: SurveillanceForm,
    val specimens: List<Specimen>,
)
