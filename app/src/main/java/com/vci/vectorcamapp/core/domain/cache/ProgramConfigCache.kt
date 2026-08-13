package com.vci.vectorcamapp.core.domain.cache

import com.vci.vectorcamapp.core.data.dto.cache.ProgramConfigCacheDto

interface ProgramConfigCache {
    suspend fun saveProgramConfig(config: ProgramConfigCacheDto)
    suspend fun getProgramConfig(): ProgramConfigCacheDto?
    suspend fun clearProgramConfig()
}
