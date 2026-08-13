package com.vci.vectorcamapp.core.data.cache

import androidx.datastore.core.DataStore
import com.vci.vectorcamapp.core.data.dto.cache.ProgramConfigCacheDto
import com.vci.vectorcamapp.core.domain.cache.ProgramConfigCache
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ProgramConfigCacheImplementation @Inject constructor(
    private val dataStore: DataStore<ProgramConfigCacheDto>
) : ProgramConfigCache {
    override suspend fun saveProgramConfig(config: ProgramConfigCacheDto) {
        dataStore.updateData { config }
    }

    override suspend fun getProgramConfig(): ProgramConfigCacheDto? {
        return dataStore.data.firstOrNull()
    }

    override suspend fun clearProgramConfig() {
        dataStore.updateData { ProgramConfigCacheDto() }
    }
}
