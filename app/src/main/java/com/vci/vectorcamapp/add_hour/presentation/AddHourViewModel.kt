package com.vci.vectorcamapp.add_hour.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions.CollectionPlaceOption
import com.vci.vectorcamapp.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddHourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

    private val destination = savedStateHandle.toRoute<Destination.AddHour>()

    private val _state = MutableStateFlow(AddHourState(parentSessionId = destination.sessionId))
    val state = _state.asStateFlow()

    private val _events = Channel<AddHourEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: AddHourAction) {
        viewModelScope.launch {
            when (action) {
                AddHourAction.ReturnToPreviousScreen -> {
                    _events.send(AddHourEvent.NavigateBackToPreviousScreen)
                }

                AddHourAction.ConfirmAddHour -> {
                    // TODO: validate and persist the hour session before navigating
                    _events.send(AddHourEvent.NavigateToImagingScreen)
                }

                is AddHourAction.SelectTimeSlot -> {
                    _state.update { it.copy(selectedTimeSlot = action.timeSlot) }
                }

                is AddHourAction.EnterWind -> {
                    _state.update { it.copy(wind = action.value) }
                }

                is AddHourAction.EnterRain -> {
                    _state.update { it.copy(rain = action.value) }
                }

                is AddHourAction.EnterRelativeHumidity -> {
                    _state.update { it.copy(relativeHumidity = action.value) }
                }

                is AddHourAction.EnterTemperature -> {
                    _state.update { it.copy(temperature = action.value) }
                }

                is AddHourAction.SelectCollectionPlace -> {
                    _state.update { it.copy(selectedCollectionPlace = action.place) }
                }
            }
        }
    }
}
