package com.vci.vectorcamapp.registration.presentation

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.data.util.sortByHierarchy
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.network.api.LocationTypeDataSource
import com.vci.vectorcamapp.core.domain.network.api.ProgramDataSource
import com.vci.vectorcamapp.core.domain.network.api.SiteDataSource
import com.vci.vectorcamapp.core.domain.network.connectivity.ConnectivityObserver
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.repository.LocationTypeRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.use_cases.collector.CollectorValidationUseCases
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.errorOrNull
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
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
    private val transactionHelper: TransactionHelper,
    private val deviceCache: DeviceCache,
    private val currentSessionCache: CurrentSessionCache,
    private val collectorRepository: CollectorRepository,
    private val collectorValidationUseCases: CollectorValidationUseCases,
    private val programDataSource: ProgramDataSource,
    private val programRepository: ProgramRepository,
    private val siteDataSource: SiteDataSource,
    private val siteRepository: SiteRepository,
    private val locationTypeDataSource: LocationTypeDataSource,
    private val locationTypeRepository: LocationTypeRepository,
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
                    fetchAndSeedAllPrograms()
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

                    if (!state.value.isConnectedToInternet) {
                        emitError(NetworkError.NO_INTERNET)
                        return@launch
                    }

                    try {
                        _state.update { it.copy(isLoading = true) }
                        transactionHelper.runAsTransaction {
                            fetchAndSeedAllLocationTypesForProgram(selectedProgram.id)
                            fetchAndSeedAllSitesForProgram(selectedProgram.id)
                        }

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
                    } finally {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun loadRegistrationDetails() {
        viewModelScope.launch {
            fetchAndSeedAllPrograms()
        }
    }

    private suspend fun fetchAndSeedAllPrograms() {
        _state.update { it.copy(isLoadingPrograms = true) }
        try {
            when (val result = programDataSource.getAllPrograms()) {
                is Result.Success -> {
                    transactionHelper.runAsTransaction {
                        result.data.programs.forEach { programDto ->
                            programRepository.upsertProgram(programDto.toDomain())
                        }
                    }
                }

                is Result.Error -> {
                    emitError(result.error)
                }
            }
        } finally {
            _state.update { it.copy(isLoadingPrograms = false) }
        }
    }

    private suspend fun fetchAndSeedAllLocationTypesForProgram(programId: Int) {
        when (val result = locationTypeDataSource.getAllLocationTypesForProgram(programId)) {
            is Result.Success -> {
                transactionHelper.runAsTransaction {
                    result.data.locationTypes.forEach { locationTypeDto ->
                        locationTypeRepository.upsertLocationType(
                            locationTypeDto.toDomain(),
                            programId
                        )
                    }
                }
            }

            is Result.Error -> {
                emitError(result.error)
            }
        }
    }

    private suspend fun fetchAndSeedAllSitesForProgram(programId: Int) {
        when (val result = siteDataSource.getAllSitesForProgram(programId)) {
            is Result.Success -> {
                transactionHelper.runAsTransaction {
                    result.data.sites.sortByHierarchy().forEach { siteDto ->
                        val locationTypeId = siteDto.locationTypeId
                        val parentId = siteDto.parentId

                        siteRepository.upsertSite(
                            siteDto.toDomain(),
                            programId,
                            locationTypeId,
                            parentId
                        )
                    }
                }
            }

            is Result.Error -> {
                emitError(result.error)
            }
        }
    }
}
