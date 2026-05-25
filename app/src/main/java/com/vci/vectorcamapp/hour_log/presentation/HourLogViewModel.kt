package com.vci.vectorcamapp.hour_log.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HourLogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

    private val destination = savedStateHandle.toRoute<Destination.HourLog>()

    private val _state = MutableStateFlow(HourLogState(sessionId = destination.sessionId))
    val state = _state.asStateFlow()

    private val _events = Channel<HourLogEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: HourLogAction) {
        viewModelScope.launch {
            when (action) {
                HourLogAction.ReturnToPreviousScreen -> {
                    _events.send(HourLogEvent.NavigateBackToPreviousScreen)
                }

                HourLogAction.NavigateToAddHour -> {
                    _events.send(HourLogEvent.NavigateToAddHourScreen(_state.value.sessionId))
                }

                is HourLogAction.ResumeHourSession -> {
                    _events.send(HourLogEvent.NavigateToImagingScreen(action.hourSessionId))
                }

                HourLogAction.UploadSession -> {
                    // TODO: trigger upload
                }
            }
        }
    }
}
