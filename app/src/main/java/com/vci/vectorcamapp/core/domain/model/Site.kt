package com.vci.vectorcamapp.core.domain.model

data class Site(
    val id: Int,
    val district: String,
    val subCounty: String,
    val parish: String,
    val villageName: String,
    val houseNumber: String,
    val healthCenter: String,
    val isActive: Boolean
)
