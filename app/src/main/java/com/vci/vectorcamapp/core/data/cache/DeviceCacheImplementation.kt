package com.vci.vectorcamapp.core.data.cache

import androidx.datastore.core.DataStore
import com.vci.vectorcamapp.core.data.dto.cache.DeviceCacheDto
import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toDto
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeviceCacheImplementation @Inject constructor(
    private val dataStore: DataStore<DeviceCacheDto>
) : DeviceCache {
    override suspend fun saveDevice(device: Device, programId: Int) {
        dataStore.updateData {
            device.toDto(programId)
        }
    }

    override suspend fun getDevice(): Device? {
        return dataStore.data.first().toDomain()
    }

    override suspend fun getProgramId(): Int? {
        val dto = dataStore.data.first()
        return if (dto.programId == -1) null else dto.programId
    }

    override fun observeDevice(): Flow<Device?> {
        return dataStore.data
            .catch { emit(DeviceCacheDto()) }
            .map { it.toDomain() }
    }

    override fun observeProgramId(): Flow<Int?> {
        return dataStore.data
            .catch { emit(DeviceCacheDto()) }
            .map { dto -> if (dto.programId == -1) null else dto.programId }
    }
}
