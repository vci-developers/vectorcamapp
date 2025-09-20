package com.vci.vectorcamapp.core.data.dto.site

import kotlinx.serialization.Serializable

@Serializable
data class SiteDto(
    val id: Int = -1,
    val programId: Int = -1,
    val district: String = "",
    val subCounty: String = "",
    val parish: String = "",
    val villageName: String = "",
    val houseNumber: String = "",
    val healthCenter: String = "",
    val isActive: Boolean = true
)
