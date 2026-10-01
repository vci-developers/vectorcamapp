package com.vci.vectorcamapp.intake.data.repository

import android.location.Location
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.data.LocationClient
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LocationRepositoryImplementationTest {

    @Test
    fun getCurrentLocation_returnsClientResult() = runTest {
        val client = mockk<LocationClient>()
        val location = mockk<Location>()
        val expected = Result.Success(location)
        coEvery { client.getCurrentLocation() } returns expected

        val result = LocationRepositoryImplementation(client).getCurrentLocation()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun getCurrentLocation_returnsClientError() = runTest {
        val client = mockk<LocationClient>()
        val expected = Result.Error(IntakeError.LOCATION_GPS_TIMEOUT)
        coEvery { client.getCurrentLocation() } returns expected

        val result = LocationRepositoryImplementation(client).getCurrentLocation()

        assertThat(result).isEqualTo(expected)
    }
}
