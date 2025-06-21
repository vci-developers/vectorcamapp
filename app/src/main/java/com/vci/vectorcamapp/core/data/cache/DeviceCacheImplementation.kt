package com.vci.vectorcamapp.core.data.cache

import androidx.datastore.core.DataStore
import com.vci.vectorcamapp.core.data.dto.DeviceDto
import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toDto
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Device
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class DeviceCacheImplementation @Inject constructor(
    private val dataStore: DataStore<DeviceDto>
) : DeviceCache {
    override suspend fun saveDevice(device: Device, programName: String) {
        dataStore.updateData {
            device.toDto(programName)
        }
    }

    override suspend fun getDevice(): Device? {
        val deviceDto = dataStore.data.firstOrNull()
        return if (deviceDto == null || deviceDto.isEmpty()) null else deviceDto.toDomain()
    }

    override suspend fun getProgramName(): String? {
        val deviceDto = dataStore.data.firstOrNull()
        return if (deviceDto == null || deviceDto.isEmpty()) null else deviceDto.programName
    }
}
