package com.vci.vectorcamapp.surveillance_form.location.data.repository

import android.Manifest
import android.location.Location
import androidx.annotation.RequiresPermission
import com.vci.vectorcamapp.surveillance_form.location.data.LocationClient
import com.vci.vectorcamapp.surveillance_form.location.domain.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

class LocationRepositoryImplementation @Inject constructor(
    private val locationClient: LocationClient
) : LocationRepository {
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override suspend fun getCurrentLocation(): Location =
        locationClient.getCurrentLocation()
}
