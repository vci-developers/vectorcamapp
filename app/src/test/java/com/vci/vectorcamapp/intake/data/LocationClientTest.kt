package com.vci.vectorcamapp.intake.data
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
class LocationClientTest {
    private val context = mockk<Context>()
    private val provider = mockk<FusedLocationProviderClient>()

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun locationRequest_requiresFreshHighAccuracyFix() = runTest {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_GRANTED
        val location = mockk<Location>()
        val task = mockk<Task<Location>>(relaxed = true)
        val request = slot<CurrentLocationRequest>()
        every { provider.getCurrentLocation(capture(request), any()) } returns task
        every { task.addOnSuccessListener(any()) } answers {
            firstArg<OnSuccessListener<Location>>().onSuccess(location)
            task
        }
        val client = LocationClient(provider, context)
        repeat(2) {
            assertThat(client.getCurrentLocation()).isEqualTo(Result.Success(location))
            assertThat(request.captured.maxUpdateAgeMillis).isEqualTo(0L)
            assertThat(request.captured.priority).isEqualTo(Priority.PRIORITY_HIGH_ACCURACY)
        }
        verify(exactly = 2) { provider.getCurrentLocation(any<CurrentLocationRequest>(), any()) }
        verify(exactly = 0) { provider.lastLocation }
    }

    @Test
    fun missingPermission_doesNotRequestLocation() = runTest {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns PackageManager.PERMISSION_DENIED
        assertThat(LocationClient(provider, context).getCurrentLocation())
            .isEqualTo(Result.Error(IntakeError.LOCATION_PERMISSION_DENIED))
        verify(exactly = 0) { provider.getCurrentLocation(any<CurrentLocationRequest>(), any()) }
    }
}
