package com.vci.vectorcamapp.core.domain.cache

import com.vci.vectorcamapp.core.data.dto.cache.DefaultIntakeFieldsCacheDto

interface DefaultIntakeFieldsCache {
    suspend fun saveDefaultIntakeFields(
        collectorName: String,
        collectorTitle: String,
        collectorLastTrainedOn: Long,
        hardwareId: String?,
        district: String,
        villageName: String,
        formAnswers: Map<Int, String>,
        locationSelections: Map<Int, String>
    )

    suspend fun getDefaultIntakeFields(): DefaultIntakeFieldsCacheDto?

    suspend fun clearDefaultIntakeFields()
}
