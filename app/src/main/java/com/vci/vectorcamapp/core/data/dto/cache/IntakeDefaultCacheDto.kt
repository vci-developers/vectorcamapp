package com.vci.vectorcamapp.core.data.dto.cache

import kotlinx.serialization.Serializable

@Serializable
data class IntakeDefaultCacheDto(
    val collectorName: String = "",
    val collectorTitle: String = "",
    val district: String = "",
    val sentinelSite: String = "",
)
