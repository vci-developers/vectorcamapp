package com.vci.vectorcamapp.core.domain.cache

import com.vci.vectorcamapp.core.data.dto.cache.IntakeDefaultCacheDto

interface IntakeDefaultCache {
    suspend fun saveIntakeDefaultValues(
        collectorName: String,
        collectorTitle: String,
        district: String,
        sentinelSite: String
    )

    suspend fun getIntakeDefaultValues(): IntakeDefaultCacheDto?

    suspend fun clearIntakeDefaultValues()
}
