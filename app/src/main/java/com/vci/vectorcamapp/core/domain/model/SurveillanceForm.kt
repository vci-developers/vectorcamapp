package com.vci.vectorcamapp.core.domain.model

import java.util.Date

data class SurveillanceForm(
    val id: String,
    val collectionDate: Date,
    val officerName: String,
    val officerTitle: String,
    val peopleInHouse: Int,
    val isBednetAvailable: Boolean,
    val numberOfBednetsAvailable: Int,
    val numberOfPeopleSleptUnderBednet: Int,
    val bednetType: String,
    val bednetBrand: String,
    val isIrsSprayed: Boolean,
    val irsDate: Date,
)
