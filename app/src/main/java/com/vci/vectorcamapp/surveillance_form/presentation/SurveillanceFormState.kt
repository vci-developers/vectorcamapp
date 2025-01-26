package com.vci.vectorcamapp.surveillance_form.presentation

import com.vci.vectorcamapp.core.domain.model.GeospatialPoint

data class SurveillanceFormState(
    val isLoading: Boolean = false,
    val locationCoordinates: GeospatialPoint = GeospatialPoint(latitude = 0f, longitude = 0f)
)
