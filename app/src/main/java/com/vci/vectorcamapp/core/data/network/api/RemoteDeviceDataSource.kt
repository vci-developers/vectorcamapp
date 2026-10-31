package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.core.data.dto.device.DeviceDto
import com.vci.vectorcamapp.core.data.dto.device.RegisterDeviceRequestDto
import com.vci.vectorcamapp.core.data.dto.device.RegisterDeviceResponseDto
import com.vci.vectorcamapp.core.data.dto.device.UpdateDeviceRequestDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.network.api.DeviceDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import javax.inject.Inject

class RemoteDeviceDataSource @Inject constructor(
    private val httpClient: HttpClient
) : DeviceDataSource {
    override suspend fun registerDevice(
        device: Device, programId: Int
    ): Result<RegisterDeviceResponseDto, NetworkError> {
        return safeCall<RegisterDeviceResponseDto> {
            httpClient.post(constructUrl("devices/register")) {
                setBody(
                    RegisterDeviceRequestDto(
                        model = device.model,
                        registeredAt = device.registeredAt,
                        programId = programId,
                        appVersion = currentAppVersion(),
                    )
                )
            }
        }
    }

    override suspend fun getDeviceById(deviceId: Int): Result<DeviceDto, NetworkError> {
        return safeCall<DeviceDto> {
            httpClient.get(constructUrl("devices/$deviceId"))
        }
    }

    override suspend fun updateAppVersionIfStale(device: DeviceDto): Result<DeviceDto, NetworkError> {
        val currentAppVersion = currentAppVersion()
        if (!DeviceAppVersion.shouldRefresh(device.deviceId, device.appVersion, currentAppVersion)) {
            return Result.Success(device)
        }
        return when (
            val result = safeCall<RegisterDeviceResponseDto> {
                httpClient.put(constructUrl("devices/${device.deviceId}")) {
                    setBody(UpdateDeviceRequestDto(appVersion = currentAppVersion))
                }
            }
        ) {
            is Result.Success -> {
                val updated = result.data.device
                if (updated.deviceId <= 0) Result.Success(device) else Result.Success(updated)
            }
            is Result.Error -> result
        }
    }

    private fun currentAppVersion(): String {
        return DeviceAppVersion.format(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
    }
}
