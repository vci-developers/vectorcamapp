package com.vci.vectorcamapp.settings.presentation

import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.landing.domain.util.LandingError
import com.vci.vectorcamapp.landing.logging.LandingSentryLogger
import com.vci.vectorcamapp.landing.presentation.LandingEvent
import com.vci.vectorcamapp.landing.presentation.LandingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor (
    private val deviceCache: DeviceCache,
    private val programRepository: ProgramRepository
): CoreViewModel(){
    private val _state = MutableStateFlow(SettingsState())

    val state: StateFlow<SettingsState> = _state
        .onStart { loadSettingsDetails() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            SettingsState()
        )

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {
                SettingsAction.StartNewDataCollectionSession -> {
                    _events.send(SettingsEvent.NavigateToIntakeScreen(SessionType.DATA_COLLECTION))
                }
                SettingsAction.ReturnToLandingScreen -> {
                    _events.send(SettingsEvent.NavigateBackToLandingScreen)
                }
            }
        }
    }

    private fun loadSettingsDetails() {
        viewModelScope.launch {
            val device = deviceCache.getDevice() ?: return@launch
            val programId = deviceCache.getProgramId() ?: return@launch
            val program = programRepository.getProgramById(programId) ?: return@launch

            _state.update {
                it.copy(
                    device = device,
                    program = program,
                )
            }
        }
    }
}
