package com.vci.vectorcamapp.location.domain.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Location
}
