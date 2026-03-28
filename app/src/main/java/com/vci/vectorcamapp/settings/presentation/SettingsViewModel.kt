package com.vci.vectorcamapp.settings.presentation

import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DefaultIntakeFieldsCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.use_cases.collector.CollectorValidationUseCases
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.LocationType
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.network.api.LocationTypeDataSource
import com.vci.vectorcamapp.core.domain.network.api.SiteDataSource
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.repository.LocationTypeRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.errorOrNull
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.settings.domain.util.SettingsError
import com.vci.vectorcamapp.settings.presentation.model.SettingsErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deviceCache: DeviceCache,
    private val programRepository: ProgramRepository,
    private val collectorRepository: CollectorRepository,
    private val collectorValidationUseCases: CollectorValidationUseCases,
    private val locationTypeDataSource: LocationTypeDataSource,
    private val siteDataSource: SiteDataSource,
    private val locationTypeRepository: LocationTypeRepository,
    private val siteRepository: SiteRepository,
    private val sessionRepository: SessionRepository,
    private val defaultIntakeFieldsCache: DefaultIntakeFieldsCache,
    private val currentSessionCache: CurrentSessionCache,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

    @Inject
    lateinit var transactionHelper: TransactionHelper

    private val _collectors = collectorRepository.observeAllCollectors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _state = MutableStateFlow(SettingsState())
    val state = combine(_collectors, _state) { collectors, state ->
        state.copy(
            collectors = collectors
        )
    }
    .onStart { loadSettingsDetails() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SettingsState())

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
                SettingsAction.ShowAddCollectorDialog -> {
                    _state.update {
                        it.copy(
                            selectedCollector = Collector(
                                id = UUID.randomUUID(),
                                name = "",
                                title = "",
                                lastTrainedOn = System.currentTimeMillis()
                            ),
                            isEditCollectorDialogVisible = false
                        )
                    }
                }
                is SettingsAction.ShowEditCollectorDialog -> {
                    _state.update {
                        it.copy(
                            selectedCollector = action.collector,
                            isEditCollectorDialogVisible = true
                        )
                    }
                }
                SettingsAction.DismissCollectorDialog -> {
                    _state.update {
                        it.copy(
                            selectedCollector = null,
                            isEditCollectorDialogVisible = false,
                            isDeleteCollectorDialogVisible = false,
                            settingsErrors = it.settingsErrors.copy(
                                collectorName = null,
                                collectorTitle = null,
                                collectorLastTrainedOn = null
                            )
                        )
                    }
                }
                is SettingsAction.EnterCollectorName -> {
                    _state.update {
                        it.copy(
                            selectedCollector = it.selectedCollector?.copy(
                                name = action.name
                            )
                        )
                    }
                }
                is SettingsAction.EnterCollectorTitle -> {
                    _state.update {
                        it.copy(
                            selectedCollector = it.selectedCollector?.copy(
                                title = action.title
                            )
                        )
                    }
                }
                is SettingsAction.EnterCollectorLastTrainedOn -> {
                    _state.update {
                        it.copy(
                            selectedCollector = it.selectedCollector?.copy(
                                lastTrainedOn = action.lastTrainedOn
                            )
                        )
                    }
                }
                SettingsAction.SaveCollector -> {
                    val collector = state.value.selectedCollector ?: return@launch

                    val nameValidationResult = collectorValidationUseCases.validateCollectorName(collector.name)
                    val titleValidationResult = collectorValidationUseCases.validateCollectorTitle(collector.title)
                    val lastTrainedOnValidationResult = collectorValidationUseCases.validateCollectorLastTrainedOn(collector.lastTrainedOn)

                    _state.update { currentState ->
                        currentState.copy(
                            settingsErrors = SettingsErrors(
                                collectorName = nameValidationResult.errorOrNull(),
                                collectorTitle = titleValidationResult.errorOrNull(),
                                collectorLastTrainedOn = lastTrainedOnValidationResult.errorOrNull()
                            )
                        )
                    }

                    val hasError = listOf(nameValidationResult, titleValidationResult, lastTrainedOnValidationResult).any { it is Result.Error }
                    if (hasError) return@launch

                    try {
                        collectorRepository.upsertCollector(collector)
                        _state.update {
                            it.copy(
                                selectedCollector = null,
                                isEditCollectorDialogVisible = false
                            )
                        }
                    } catch (e: Exception) {
                        emitError(SettingsError.COLLECTOR_SAVE_FAILED)
                    }
                }
                SettingsAction.ShowDeleteCollectorDialog -> {
                    _state.update {
                        it.copy(isDeleteCollectorDialogVisible = true)
                    }
                }
                SettingsAction.DismissDeleteCollectorDialog -> {
                    _state.update {
                        it.copy(isDeleteCollectorDialogVisible = false)
                    }
                }
                SettingsAction.ConfirmDeleteCollector -> {
                    val collector = state.value.selectedCollector ?: return@launch
                    try {
                        collectorRepository.deleteCollector(collector)
                        _state.update {
                            it.copy(
                                selectedCollector = null,
                                isEditCollectorDialogVisible = false,
                                isDeleteCollectorDialogVisible = false
                            )
                        }
                    } catch (e: Exception) {
                        emitError(SettingsError.COLLECTOR_DELETION_FAILED)
                    }
                }
                SettingsAction.ResyncLocations -> {
                    resyncLocations()
                }
            }
        }
    }

    private suspend fun resyncLocations() {
        _state.update { it.copy(isSyncingLocations = true) }
        try {
            val programId = state.value.program?.id ?: return

            val locationTypesResult = locationTypeDataSource.getAllLocationTypesForProgram(programId)
            if (locationTypesResult is Result.Error) {
                emitError(SettingsError.LOCATION_SYNC_FAILED)
                return
            }
            val locationTypeDtos = (locationTypesResult as Result.Success).data.locationTypes

            val sitesResult = siteDataSource.getAllSitesForProgram(programId)
            if (sitesResult is Result.Error) {
                emitError(SettingsError.LOCATION_SYNC_FAILED)
                return
            }
            val siteDtos = (sitesResult as Result.Success).data

            val incompleteSessions = sessionRepository.observeIncompleteSessionsAndSites().first()
            val currentSession = currentSessionCache.getSession()
            val defaultFields = defaultIntakeFieldsCache.getDefaultIntakeFields()

            val success = transactionHelper.runAsTransaction {
                val groupedLocationTypes = locationTypeDtos.groupBy { it.level }.toSortedMap()
                for ((_, typesAtLevel) in groupedLocationTypes) {
                    typesAtLevel.forEach { dto ->
                        val domainType = LocationType(
                            id = dto.id,
                            name = dto.name,
                            level = dto.level
                        )
                        locationTypeRepository.upsertLocationType(domainType, programId)
                    }
                }

                siteDtos.forEach { dto ->
                    val domainSite = Site(
                        id = dto.siteId,
                        district = dto.district,
                        subCounty = dto.subCounty,
                        parish = dto.parish,
                        villageName = dto.villageName,
                        houseNumber = dto.houseNumber,
                        healthCenter = dto.healthCenter,
                        isActive = dto.isActive,
                        name = dto.name,
                        locationHierarchy = dto.locationHierarchy
                    )
                    siteRepository.upsertSite(
                        site = domainSite,
                        programId = programId,
                        locationTypeId = dto.locationTypeId,
                        parentId = dto.parentId
                    )
                }

                incompleteSessions.forEach { sessionAndSite ->
                    sessionRepository.upsertSession(sessionAndSite.session, siteId = -1)
                }

                true
            }

            if (success) {
                if (defaultFields != null) {
                    defaultIntakeFieldsCache.saveDefaultIntakeFields(
                        collectorName = defaultFields.collectorName,
                        collectorTitle = defaultFields.collectorTitle,
                        collectorLastTrainedOn = defaultFields.collectorLastTrainedOn,
                        hardwareId = defaultFields.hardwareId,
                        district = "",
                        villageName = ""
                    )
                }

                if (currentSession != null) {
                    currentSessionCache.saveSession(currentSession, siteId = -1)
                }
            } else {
                emitError(SettingsError.LOCATION_SYNC_FAILED)
            }
        } catch (e: Exception) {
            emitError(SettingsError.LOCATION_SYNC_FAILED)
        } finally {
            _state.update { it.copy(isSyncingLocations = false) }
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
