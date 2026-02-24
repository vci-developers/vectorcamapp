package com.vci.vectorcamapp.settings.presentation

import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.use_cases.collector.CollectorValidationUseCases
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
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
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

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
