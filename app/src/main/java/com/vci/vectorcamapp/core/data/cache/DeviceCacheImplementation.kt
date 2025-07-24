package com.vci.vectorcamapp.core.data.cache

import androidx.datastore.core.DataStore
import com.vci.vectorcamapp.core.data.dto.cache.DeviceCacheDto
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
            DeviceCacheDto(
                id = device.id,
                programId = programId,
                model = device.model,
                registeredAt = device.registeredAt,
                submittedAt = device.submittedAt
            )
        }
    }

    override suspend fun getDevice(): Device? {
        val dto = dataStore.data.first()
        return if (dto.programId == -1) {
            null
        } else {
            Device(
                id = dto.id,
                model = dto.model,
                registeredAt = dto.registeredAt,
                submittedAt = dto.submittedAt
            )
        }
    }

    override suspend fun getProgramId(): Int? {
        val dto = dataStore.data.first()
        return if (dto.programId == -1) null else dto.programId
    }

    override fun observeDevice(): Flow<Device?> {
        return dataStore.data
            .catch { emit(DeviceCacheDto()) }
            .map {
                if (it.programId == -1) {
                    null
                }
                else {
                    Device(
                        id = it.id,
                        model = it.model,
                        registeredAt = it.registeredAt,
                        submittedAt = it.submittedAt
                    )
                }
            }
    }

    override fun observeProgramId(): Flow<Int?> {
        return dataStore.data
            .catch { emit(DeviceCacheDto()) }
            .map { dto -> if (dto.programId == -1) null else dto.programId }
    }
}
