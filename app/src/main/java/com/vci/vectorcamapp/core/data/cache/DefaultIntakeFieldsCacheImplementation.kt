    package com.vci.vectorcamapp.core.data.cache

    import androidx.datastore.core.DataStore
    import com.vci.vectorcamapp.core.data.dto.cache.DefaultIntakeFieldsCacheDto
    import com.vci.vectorcamapp.core.domain.cache.DefaultIntakeFieldsCache
    import kotlinx.coroutines.flow.firstOrNull
    import javax.inject.Inject

    class DefaultIntakeFieldsCacheImplementation @Inject constructor(
        private val dataStore: DataStore<DefaultIntakeFieldsCacheDto>
    ) : DefaultIntakeFieldsCache {
        override suspend fun saveDefaultIntakeFields(
            collectorName: String,
            collectorTitle: String,
            district: String,
            villageName: String,
            houseNumber: String
        ) {
            dataStore.updateData {
                DefaultIntakeFieldsCacheDto(
                    collectorName = collectorName,
                    collectorTitle = collectorTitle,
                    district = district,
                    villageName = villageName,
                    houseNumber = houseNumber
                )
            }
        }

        override suspend fun getDefaultIntakeFields(): DefaultIntakeFieldsCacheDto? {
            return dataStore.data.firstOrNull()
        }

        override suspend fun clearDefaultIntakeFields() {
            dataStore.updateData {
                DefaultIntakeFieldsCacheDto()
            }
        }
    }
