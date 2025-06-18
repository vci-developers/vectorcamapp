package com.vci.vectorcamapp.registration.presentation

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
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    private val _events = Channel<RegistrationEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            programRepository.getAllPrograms()
                .onStart { _state.update { it.copy(isLoading = true) } }
                .catch { e -> _state.update { it.copy(isLoading = false, error = e.localizedMessage) } }
                .collect { list ->
                    _state.update { it.copy(isLoading = false, programs = list, error = null) }
                }
        }
    }

    fun onAction(action: RegistrationAction) {
        when (action) {
            is RegistrationAction.RegisterProgram -> {
                viewModelScope.launch {
                    // TODO: use real data instead
                    val device = Device(
                        id = 1,
                        model = "fake phone",
                        registeredAt = System.currentTimeMillis()
                    )
                    deviceCache.saveDevice(device, action.programId)
                    _events.send(RegistrationEvent.NavigateToLanding)
                }
            }
        }
    }
}