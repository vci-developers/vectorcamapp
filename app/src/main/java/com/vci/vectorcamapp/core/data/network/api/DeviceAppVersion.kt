package com.vci.vectorcamapp.core.data.network.api

internal object DeviceAppVersion {
    fun format(versionCode: Int, versionName: String): String {
        return "$versionCode($versionName)"
    }

    fun shouldRefresh(
        deviceId: Int,
        recordedAppVersion: String?,
        currentAppVersion: String,
    ): Boolean {
        return deviceId > 0 && recordedAppVersion != currentAppVersion
    }
}
