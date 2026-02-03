package com.vci.vectorcamapp.core.data.dto.site

import kotlinx.serialization.Serializable

@Serializable
data class SiteDto(
    val id: Int = -1,
    val programId: Int = -1,
    val district: String? = null,
    val subCounty: String? = null,
    val parish: String? = null,
    val villageName: String? = null,
    val houseNumber: String? = null,
    val healthCenter: String? = null,
    val isActive: Boolean = true,
    val locationTypeId: Int? = null,
    val parentId: Int? = null,
    val name: String? = null,
    val locationHierarchy: Map<String, String>? = null
)
