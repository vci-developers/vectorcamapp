package com.vci.vectorcamapp.registration.presentation

import android.os.Build
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.network.api.RemoteProgramDataSource
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.network.connectivity.ConnectivityObserver
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.use_cases.collector.CollectorValidationUseCases
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.errorOrNull
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.registration.domain.util.RegistrationError
import com.vci.vectorcamapp.registration.logging.RegistrationSentryLogger
import com.vci.vectorcamapp.registration.presentation.model.RegistrationErrors
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
class RegistrationViewModel @Inject constructor(
    private val deviceCache: DeviceCache,
    private val currentSessionCache: CurrentSessionCache,
    private val collectorRepository: CollectorRepository,
    private val collectorValidationUseCases: CollectorValidationUseCases,
    private val remoteProgramDataSource: RemoteProgramDataSource,
    programRepository: ProgramRepository,
    connectivityObserver: ConnectivityObserver
) : CoreViewModel() {

    private val _isConnectedToInternet = connectivityObserver.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val _allPrograms = programRepository.observeAllPrograms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _state = MutableStateFlow(RegistrationState())
    val state: StateFlow<RegistrationState> = combine(
        _isConnectedToInternet,
        _allPrograms,
        _state
    ) { isConnectedToInternet, allPrograms, state ->
        state.copy(
            isConnectedToInternet = isConnectedToInternet,
            programs = allPrograms
        )
    }.onStart {
        loadRegistrationDetails()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RegistrationState())

    private val _events = Channel<RegistrationEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: RegistrationAction) {
        viewModelScope.launch {
            when (action) {
                is RegistrationAction.SelectProgram -> {
                    _state.update { it.copy(selectedProgram = action.program) }
                }

                is RegistrationAction.EnterCollectorName -> {
                    _state.update {
                        it.copy(
                            collector = it.collector.copy(
                                name = action.text
                            )
                        )
                    }
                }


                is RegistrationAction.EnterCollectorTitle -> {
                    _state.update {
                        it.copy(
                            collector = it.collector.copy(
                                title = action.text
                            )
                        )
                    }
                }

                is RegistrationAction.EnterCollectorLastTrainedOn -> {
                    _state.update {
                        it.copy(
                            collector = it.collector.copy(
                                lastTrainedOn = action.lastTrainedOn
                            )
                        )
                    }
                }

                RegistrationAction.RefreshPrograms -> {
                    fetchAllPrograms()
                }

                RegistrationAction.ConfirmRegistration -> {
                    val selectedProgram = state.value.selectedProgram

                    val collector = state.value.collector
                    val collectorNameValidationResult =
                        collectorValidationUseCases.validateCollectorName(collector.name)
                    val collectorTitleValidationResult =
                        collectorValidationUseCases.validateCollectorTitle(collector.title)
                    val collectorLastTrainedOnValidationResult =
                        collectorValidationUseCases.validateCollectorLastTrainedOn(collector.lastTrainedOn)

                    _state.update { currentState ->
                        currentState.copy(
                            registrationErrors = RegistrationErrors(
                                collectorName = collectorNameValidationResult.errorOrNull(),
                                collectorTitle = collectorTitleValidationResult.errorOrNull(),
                                collectorLastTrainedOn = collectorLastTrainedOnValidationResult.errorOrNull()
                            )
                        )
                    }

                    val hasError = listOf(
                        collectorNameValidationResult,
                        collectorTitleValidationResult,
                        collectorLastTrainedOnValidationResult
                    ).any { it is Result.Error }
                    if (hasError) return@launch

                    if (selectedProgram == null) {
                        emitError(RegistrationError.PROGRAM_NOT_FOUND)
                        RegistrationSentryLogger.logProgramNotFound(IllegalStateException("Program not found during registration"))
                        return@launch
                    }

                    try {
                        val device = Device(
                            id = -1,
                            model = "${Build.MANUFACTURER} ${Build.MODEL}",
                            registeredAt = System.currentTimeMillis(),
                            submittedAt = null,
                        )
                        deviceCache.saveDevice(device, selectedProgram.id)
                        currentSessionCache.clearSession()
                        collectorRepository.upsertCollector(_state.value.collector)
                        _events.send(RegistrationEvent.NavigateToLandingScreen)
                    } catch (e: Exception) {
                        emitError(RegistrationError.UNKNOWN_ERROR)
                        RegistrationSentryLogger.logDeviceRegistrationFailure(e, selectedProgram.id)
                    }
                }
            }
        }
    }

    private fun loadRegistrationDetails() {
        viewModelScope.launch {
            fetchAllPrograms()
        }
    }
    
    private suspend fun fetchAllPrograms() {
        _state.update { it.copy(isLoadingPrograms = true) }
        try {
            when (val result = remoteProgramDataSource.getAllPrograms()) {
                is Result.Success -> {
                    result.data.programs.forEach {
                        Log.d("RegistrationViewModel", "loadRegistrationDetails: $it")
                    }
                }
                is Result.Error -> {
                    Log.d("RegistrationViewModel", "loadRegistrationDetails: ${result.error}")
                    emitError(result.error)
                }
            }
        } finally {
            _state.update { it.copy(isLoadingPrograms = false) }
        }
    }
}
