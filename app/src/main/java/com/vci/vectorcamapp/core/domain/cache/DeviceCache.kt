package com.vci.vectorcamapp.core.domain.cache

import com.vci.vectorcamapp.core.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceCache {
    suspend fun saveDevice(device: Device, programName: String)
    suspend fun getDevice(): Device?
    suspend fun getProgramName(): String?
}
