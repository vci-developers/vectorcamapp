package com.vci.vectorcamapp.core.domain.cache

import com.vci.vectorcamapp.core.data.dto.cache.DefaultIntakeFieldsCacheDto

interface DefaultIntakeFieldsCache {
    suspend fun saveDefaultIntakeFields(
        collectorName: String,
        collectorTitle: String,
        district: String,
        sentinelSite: String
    )

    suspend fun getDefaultIntakeFields(): DefaultIntakeFieldsCacheDto?

    suspend fun clearDefaultIntakeFields()
}
