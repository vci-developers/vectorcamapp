package com.vci.vectorcamapp.registration.presentation

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.mappers.toDomain
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
    private val programRepository: ProgramRepository,
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

                    try {
                        // TODO: Fetch and seed sites for selected program (CHANGE THE STRUCTURE IF YOU ARE NOT HAPPY WITH THE WAY THIS IS SET UP)
                        // TODO: IF THERE IS NO INTERNET CONNECTIVITY, THE USER SHOULD NOT BE ABLE TO CONFIRM AND MOVE TO THE LANDING SCREEN. EMIT A NO_INTERNET ERROR
                        // Call this BEFORE completing registration to ensure sites are available
                        fetchAndSeedAllSitesForProgram(selectedProgram.id)
                        
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
            fetchAndSeedAllPrograms()
        }
    }

    private suspend fun fetchAndSeedAllPrograms() {
        _state.update { it.copy(isLoadingPrograms = true) }
        try {
            when (val result = remoteProgramDataSource.getAllPrograms()) {
                is Result.Success -> {
                    val programs = result.data.programs.map { it.toDomain() }
                    programRepository.upsertAllPrograms(programs)
                }

                is Result.Error -> {
                    emitError(result.error)
                }
            }
        } finally {
            _state.update { it.copy(isLoadingPrograms = false) }
        }
    }

    /**
     * Fetches sites for the selected program and saves them to the database.
     * Called when user confirms registration to seed initial site data.
     * 
     * TODO: Implementation Steps:
     * 1. Create RemoteSiteDataSource with getSitesForProgram(programId: Int) API call
     * 2. Add SiteRepository with upsertAllSites(sites: List<Site>) method
     * 3. In SiteDao, add @Upsert suspend fun upsertAllSites(sites: List<SiteEntity>)
     * 4. Create Site mapper: SiteDto.toDomain() and Site.toEntity()
     * 5. Inject RemoteSiteDataSource and SiteRepository into this ViewModel
     * 6. Add loading state for sites (isLoadingSites) if needed
     * 7. Handle errors appropriately (network failure, database error)
     * 8. Consider adding Sentry logging for site fetch failures
     */
    private suspend fun fetchAndSeedAllSitesForProgram(programId: Int) {
        // TODO: Implement site fetching
        // Example implementation:
        // _state.update { it.copy(isLoadingSites = true) }
        // try {
        //     when (val result = remoteSiteDataSource.getSitesForProgram(programId)) {
        //         is Result.Success -> {
        //             val sites = result.data.sites.map { it.toDomain() }
        //             siteRepository.upsertAllSites(sites)
        //         }
        //         is Result.Error -> {
        //             emitError(result.error)
        //             RegistrationSentryLogger.logSiteFetchFailure(programId, result.error)
        //         }
        //     }
        // } finally {
        //     _state.update { it.copy(isLoadingSites = false) }
        // }
    }
}
