package com.vci.vectorcamapp.core.domain.model

data class SurveillanceForm(
    val country: String,
    val district: String,
    val healthCenter: String,
    val sentinelSite: String,
    val householdNumber: String,
    val latitude: Float,
    val longitude: Float,
    val collectionDate: Long,
    val collectionMethod: String,
    val collectorName: String,
    val collectorTitle: String,
    val numPeopleSleptInHouse: Int,
    val wasIrsConducted: Boolean,
    val monthsSinceIrs: Int?,
    val numLlinsAvailable: Int,
    val llinType: String?,
    val llinBrand: String?,
    val numPeopleSleptUnderLlin: Int?,
    val notes: String,
)
