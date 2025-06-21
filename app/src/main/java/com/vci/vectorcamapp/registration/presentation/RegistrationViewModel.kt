package com.vci.vectorcamapp.registration.presentation

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val programRepository: ProgramRepository,
    private val deviceCache: DeviceCache
) : ViewModel() {

    private val _state = MutableStateFlow(RegistrationState())
    val state: StateFlow<RegistrationState> = _state.onStart {
        loadAllPrograms()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        RegistrationState()
    )

    private val _events = Channel<RegistrationEvent>()
    val events = _events.receiveAsFlow()

    private fun loadAllPrograms() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val programs = programRepository.getAllPrograms()
            _state.update {
                it.copy(
                    programs = programs,
                    isLoading = false
                )
            }
        }
    }

    fun onAction(action: RegistrationAction) {
        when (action) {
            is RegistrationAction.SelectProgram -> {
                _state.update { it.copy(selectedProgram = action.option.label) }
            }

            RegistrationAction.ConfirmRegistration -> {
                val programId = _state.value.selectedProgram ?: return
                //TODO: Dummy Device, replace later
                viewModelScope.launch {
                    val device = Device(
                        id = -1,
                        model = "${Build.MANUFACTURER} ${Build.MODEL}",
                        registeredAt = 0L
                    )
                    deviceCache.saveDevice(device, programId)
                    _events.send(RegistrationEvent.NavigateToLandingScreen)
                }
            }
        }
    }
}