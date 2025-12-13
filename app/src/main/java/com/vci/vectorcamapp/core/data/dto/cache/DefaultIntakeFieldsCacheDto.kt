package com.vci.vectorcamapp.core.data.dto.cache

import kotlinx.serialization.Serializable

@Serializable
data class DefaultIntakeFieldsCacheDto(
    val collectorName: String = "",
    val collectorTitle: String = "",
    val collectorLastTrainedOn: Long = 0L,
    val hardwareId: String? = null,
    val district: String = "",
    val villageName: String = "",
)
