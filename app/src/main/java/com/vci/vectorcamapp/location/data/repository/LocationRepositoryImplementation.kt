package com.vci.vectorcamapp.location.data.repository

import android.location.Location
import com.vci.vectorcamapp.location.data.LocationClient
import com.vci.vectorcamapp.location.domain.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImplementation @Inject constructor(
    private val locationClient: LocationClient
) : LocationRepository {
    override suspend fun getCurrentLocation(): Location =
        locationClient.getCurrentLocation()
}
