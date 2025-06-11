package com.vci.vectorcamapp.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TempDeviceDto(
    val siteId: Int,
)

@Serializable
data class TempSiteDto(
    val latitude: Float,
    val longitude: Float,
    val villageName: String,
    val houseNumber: Int,
    val healthCenterId: Int
)

@Serializable
data class TempHealthCenterDto(
    val latitude: Float,
    val longitude: Float,
    val parish: String,
    val subcounty: String,
    val district: String,
    val country: String
)

// Device ID = 2
val tempDeviceDto = TempDeviceDto(
    siteId = 2
)

val tempSiteDto = TempSiteDto(
    latitude = 0.25f,
    longitude = 0.25f,
    villageName = "Village1",
    houseNumber = 1,
    healthCenterId = 2
)

val tempHealthCenterDto = TempHealthCenterDto(
    latitude = 0.5f,
    longitude = 0.5f,
    parish = "Parish1",
    subcounty = "Subcounty1",
    district = "District1",
    country = "Country1"
)
