package com.vci.vectorcamapp.core.data.network.api

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeviceAppVersionTest {

    @Test
    fun format_matchesRegistrationPayload() {
        assertThat(DeviceAppVersion.format(versionCode = 42, versionName = "1.4.0"))
            .isEqualTo("42(1.4.0)")
    }

    @Test
    fun shouldRefresh_whenCachedDeviceIsMissingFromTheBackend() {
        assertThat(
            DeviceAppVersion.shouldRefresh(
                deviceId = -1,
                recordedAppVersion = null,
                currentAppVersion = "42(1.4.0)",
            )
        ).isFalse()
    }

    @Test
    fun shouldRefresh_whenRecordedVersionIsMissingOrOld() {
        assertThat(
            DeviceAppVersion.shouldRefresh(
                deviceId = 7,
                recordedAppVersion = null,
                currentAppVersion = "42(1.4.0)",
            )
        ).isTrue()
        assertThat(
            DeviceAppVersion.shouldRefresh(
                deviceId = 7,
                recordedAppVersion = "41(1.3.0)",
                currentAppVersion = "42(1.4.0)",
            )
        ).isTrue()
    }

    @Test
    fun shouldRefresh_whenRecordedVersionIsAlreadyCurrent() {
        assertThat(
            DeviceAppVersion.shouldRefresh(
                deviceId = 7,
                recordedAppVersion = "42(1.4.0)",
                currentAppVersion = "42(1.4.0)",
            )
        ).isFalse()
    }
}
