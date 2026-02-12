package com.vci.vectorcamapp.landing.presentation

import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.data.util.sortByHierarchy
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.network.api.LocationTypeDataSource
import com.vci.vectorcamapp.core.domain.network.api.SiteDataSource
import com.vci.vectorcamapp.core.domain.repository.LocationTypeRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.landing.domain.util.LandingError
import com.vci.vectorcamapp.landing.logging.LandingSentryLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val transactionHelper: TransactionHelper,
    private val deviceCache: DeviceCache,
    private val currentSessionCache: CurrentSessionCache,
    private val programRepository: ProgramRepository,
    private val siteDataSource: SiteDataSource,
    private val siteRepository: SiteRepository,
    private val locationTypeDataSource: LocationTypeDataSource,
    private val locationTypeRepository: LocationTypeRepository,
    sessionRepository: SessionRepository,
) : CoreViewModel() {

    private val _incompleteSessionsCount = sessionRepository.observeIncompleteSessionsAndSites()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    private val _state = MutableStateFlow(LandingState())
    val state: StateFlow<LandingState> = combine(
        _state,
        _incompleteSessionsCount
    ) { state, incompleteSessionsCount ->
        state.copy(incompleteSessionsCount = incompleteSessionsCount)
    }.onStart {
        loadLandingDetails()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        LandingState()
    )

    private val _events = Channel<LandingEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: LandingAction) {
        viewModelScope.launch {
            when (action) {
                LandingAction.StartNewSurveillanceSession -> {
                    _events.send(LandingEvent.NavigateToIntakeScreen(SessionType.SURVEILLANCE))
                }

                LandingAction.ViewIncompleteSessions -> {
                    _events.send(LandingEvent.NavigateToIncompleteSessionsScreen)
                }

                LandingAction.ViewCompleteSessions -> {
                    _events.send(LandingEvent.NavigateToCompleteSessionsScreen)
                }

                LandingAction.OpenSettings -> {
                    _events.send(LandingEvent.NavigateToSettingsScreen)
                }

                LandingAction.ResumeSession -> {
                    val session = currentSessionCache.getSession()
                    if (session == null) {
                        emitError(LandingError.SESSION_NOT_FOUND)
                        LandingSentryLogger.logSessionNotFound(Exception(LandingError.SESSION_NOT_FOUND.name))
                        return@launch
                    }

                    _state.update { it.copy(showResumeDialog = false) }
                    _events.send(LandingEvent.NavigateToIntakeScreen(session.type))
                }

                LandingAction.DismissResumePrompt -> {
                    currentSessionCache.clearSession()
                    _state.update { it.copy(showResumeDialog = false) }
                }

                LandingAction.RefreshSites -> {
                    val programId = deviceCache.getProgramId()
                    if (programId == null) {
                        emitError(LandingError.PROGRAM_NOT_FOUND)
                        _events.send(LandingEvent.NavigateBackToRegistrationScreen)
                        LandingSentryLogger.logProgramIdNotFound(Exception(LandingError.PROGRAM_NOT_FOUND.name))
                        return@launch
                    }

                    refreshAllSitesForProgram(programId)
                }
            }
        }
    }

    private fun loadLandingDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val programId = deviceCache.getProgramId()
            if (programId == null) {
                emitError(LandingError.PROGRAM_NOT_FOUND)
                _events.send(LandingEvent.NavigateBackToRegistrationScreen)
                _state.update { it.copy(isLoading = false) }
                LandingSentryLogger.logProgramIdNotFound(Exception(LandingError.PROGRAM_NOT_FOUND.name))
                return@launch
            }

            val program = programRepository.getProgramById(programId)
            if (program == null) {
                emitError(LandingError.PROGRAM_NOT_FOUND)
                _events.send(LandingEvent.NavigateBackToRegistrationScreen)
                _state.update { it.copy(isLoading = false) }
                LandingSentryLogger.logProgramNotFound(
                    Exception(LandingError.PROGRAM_NOT_FOUND.name),
                    programId
                )
                return@launch
            }

            transactionHelper.runAsTransaction {
                refreshAllLocationTypesForProgram(programId)
                refreshAllSitesForProgram(programId)
            }

            val currentSession = currentSessionCache.getSession()
            if (currentSession != null) {
                _state.update { it.copy(showResumeDialog = true) }
            }

            _state.update {
                it.copy(
                    enrolledProgram = program, isLoading = false
                )
            }
        }
    }

    private suspend fun refreshAllLocationTypesForProgram(
        programId: Int
    ) {
        locationTypeDataSource.getAllLocationTypesForProgram(programId).onSuccess { data ->
            transactionHelper.runAsTransaction {
                data.locationTypes.forEach { locationtypeDto ->
                    locationTypeRepository.upsertLocationType(locationtypeDto.toDomain(), programId)
                }
            }
        }
    }

    private suspend fun refreshAllSitesForProgram(
        programId: Int
    ) {
        siteDataSource.getAllSitesForProgram(programId).onSuccess { data ->
            transactionHelper.runAsTransaction {
                data.sites.sortByHierarchy().forEach { siteDto ->
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
    }
}

