package com.vci.vectorcamapp.add_hour.presentation

import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions.CollectionPlaceOption

sealed interface AddHourAction {
    data object ReturnToPreviousScreen : AddHourAction
    data object ConfirmAddHour : AddHourAction
    data class SelectTimeSlot(val timeSlot: String) : AddHourAction
    data class EnterWind(val value: String) : AddHourAction
    data class EnterRain(val value: String) : AddHourAction
    data class EnterRelativeHumidity(val value: String) : AddHourAction
    data class EnterTemperature(val value: String) : AddHourAction
    data class SelectCollectionPlace(val place: CollectionPlaceOption) : AddHourAction
}
