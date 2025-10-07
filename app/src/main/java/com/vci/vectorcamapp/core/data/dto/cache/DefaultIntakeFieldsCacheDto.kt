package com.vci.vectorcamapp.core.data.dto.cache

import kotlinx.serialization.Serializable

@Serializable
data class DefaultIntakeFieldsCacheDto(
    val collectorName: String = "",
    val collectorTitle: String = "",
    val district: String = "",
    val villageName: String = "",
    val houseNumber: String = ""
)
