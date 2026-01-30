package com.vci.vectorcamapp.core.domain.model

data class Site(
    val id: Int,
    val district: String? = null,
    val subCounty: String? = null,
    val parish: String? = null,
    val villageName: String? = null,
    val houseNumber: String? = null,
    val healthCenter: String? = null,
    val isActive: Boolean,
    val locationTypeId: Int? = null,
    val parentId: Int? = null,
    val name: String? = null,
    val locationHierarchy: String? = null
)
