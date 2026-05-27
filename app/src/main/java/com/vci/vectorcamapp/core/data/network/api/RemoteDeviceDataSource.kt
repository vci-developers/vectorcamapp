package com.vci.vectorcamapp.core.data.network.api

import android.content.Context
import android.provider.Settings
import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.core.data.dto.device.DeviceDto
import com.vci.vectorcamapp.core.data.dto.device.RegisterDeviceRequestDto
import com.vci.vectorcamapp.core.data.dto.device.RegisterDeviceResponseDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.network.api.DeviceDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import javax.inject.Inject

class RemoteDeviceDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
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
                        appVersion = BuildConfig.VERSION_CODE.toString() + "(" + BuildConfig.VERSION_NAME + ")",
                        ssaid = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
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
}
