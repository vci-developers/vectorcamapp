package com.vci.vectorcamapp.core.domain.model

data class Site(
    val id: Int,
    val district: String,
    val subCounty: String,
    val parish: String,
    val villageName: String,
    val houseNumber: String,
    val healthCenter: String,
    val isActive: Boolean,
    val locationTypeId: Int? = null,
    val parentId: Int? = null,
    val name: String? = null,
    val locationHierarchy: String? = null
)
