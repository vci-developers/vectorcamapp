    package com.vci.vectorcamapp.core.data.cache

    import androidx.datastore.core.DataStore
    import com.vci.vectorcamapp.core.data.dto.cache.IntakeDefaultCacheDto
    import com.vci.vectorcamapp.core.domain.cache.IntakeDefaultCache
    import kotlinx.coroutines.flow.firstOrNull
    import javax.inject.Inject

    class IntakeDefaultCacheImplementation @Inject constructor(
        private val dataStore: DataStore<IntakeDefaultCacheDto>
    ) : IntakeDefaultCache {
        override suspend fun saveIntakeDefaultValues(
            collectorName: String,
            collectorTitle: String,
            district: String,
            sentinelSite: String
        ) {
            dataStore.updateData {
                IntakeDefaultCacheDto(
                    collectorName = collectorName,
                    collectorTitle = collectorTitle,
                    district = district,
                    sentinelSite = sentinelSite
                )
            }
        }

        override suspend fun getIntakeDefaultValues(): IntakeDefaultCacheDto? {
            return dataStore.data.firstOrNull()
        }

        override suspend fun clearIntakeDefaultValues() {
            dataStore.updateData {
                IntakeDefaultCacheDto()
            }
        }
    }
