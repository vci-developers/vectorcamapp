package com.vci.vectorcamapp.add_hour.presentation

import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions.CollectionPlaceOption

data class AddHourState(
    val isLoading: Boolean = false,
    val parentSessionId: String = "",
    val selectedTimeSlot: String? = null,
    val wind: String = "",
    val rain: String = "",
    val relativeHumidity: String = "",
    val temperature: String = "",
    val selectedCollectionPlace: CollectionPlaceOption? = null
)
