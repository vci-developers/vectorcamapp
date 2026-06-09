package com.vci.vectorcamapp.registration.presentation

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.dto.form_question.FormQuestionDto
import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.data.util.sortByHierarchy
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Device
import com.vci.vectorcamapp.core.domain.model.Program
import com.vci.vectorcamapp.core.domain.network.api.FormDataSource
import com.vci.vectorcamapp.core.domain.network.api.LocationTypeDataSource
import com.vci.vectorcamapp.core.domain.network.api.ProgramDataSource
import com.vci.vectorcamapp.core.domain.network.api.SiteDataSource
import com.vci.vectorcamapp.core.domain.network.connectivity.ConnectivityObserver
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.repository.LocationTypeRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.use_cases.collector.CollectorValidationUseCases
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.errorOrNull
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.logging.Crashy
import com.vci.vectorcamapp.core.logging.VectorCamAnalytics
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.registration.domain.util.RegistrationError
import com.vci.vectorcamapp.registration.logging.RegistrationErrorLogger
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
    private val formDataSource: FormDataSource,
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository,
    connectivityObserver: ConnectivityObserver,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

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
                    VectorCamAnalytics.logEvent(
                        "registration_program_selected",
                        mapOf(
                            "program_id" to action.program.id,
                            "program_name" to action.program.name
                        )
                    )
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

                is RegistrationAction.EnterProgramAccessCode -> {
                    _state.update {
                        it.copy(
                            programAccessCodeInput = action.accessCode,
                            programAccessCodeError = null
                        )
                    }
                }

                RegistrationAction.DismissProgramAccessCodeDialog -> {
                    VectorCamAnalytics.logEvent("registration_access_code_dialog_dismissed")
                    _state.update {
                        it.copy(
                            isProgramAccessCodeDialogVisible = false,
                            programAccessCodeInput = "",
                            programAccessCodeError = null
                        )
                    }
                }

                RegistrationAction.RefreshPrograms -> {
                    VectorCamAnalytics.logEvent("registration_programs_refresh_clicked")
                    fetchAndSeedAllPrograms()
                }

                RegistrationAction.ConfirmRegistration -> {
                    val selectedProgram = _state.value.selectedProgram
                    val collector = _state.value.collector
                    VectorCamAnalytics.logEvent(
                        "registration_confirm_clicked",
                        mapOf(
                            "has_selected_program" to (selectedProgram != null),
                            "collector_name_present" to collector.name.isNotBlank(),
                            "collector_title_present" to collector.title.isNotBlank(),
                            "collector_last_trained_on_present" to (collector.lastTrainedOn != null)
                        )
                    )

                    if (selectedProgram == null) {
                        emitError(RegistrationError.PROGRAM_NOT_FOUND)
                        RegistrationErrorLogger.logProgramNotFound(IllegalStateException("Program not found during registration"))
                        return@launch
                    }

                    if (!validateCollectorInputs()) {
                        return@launch
                    }

                    _state.update {
                        it.copy(
                            isProgramAccessCodeDialogVisible = true,
                            programAccessCodeInput = "",
                            programAccessCodeError = null
                        )
                    }
                }

                RegistrationAction.SubmitProgramAccessCode -> {
                    val selectedProgram = _state.value.selectedProgram
                    if (selectedProgram == null) {
                        emitError(RegistrationError.PROGRAM_NOT_FOUND)
                        RegistrationErrorLogger.logProgramNotFound(
                            IllegalStateException("Program not found during access code submission")
                        )
                        return@launch
                    }
                    VectorCamAnalytics.logEvent(
                        "registration_access_code_submitted",
                        mapOf(
                            "program_id" to selectedProgram.id,
                            "code_length" to _state.value.programAccessCodeInput.length
                        )
                    )

                    if (!_isConnectedToInternet.value) {
                        emitError(NetworkError.NO_INTERNET)
                        return@launch
                    }

                    val programAccessCode = _state.value.programAccessCodeInput
                    _state.update {
                        it.copy(
                            isLoading = true,
                            programAccessCodeError = null
                        )
                    }

                    val verifyStartMs = System.currentTimeMillis()
                    when (val result = programDataSource.verifyAccessCode(selectedProgram.id, programAccessCode)) {
                        is Result.Success -> {
                            val verifyDurationMs = System.currentTimeMillis() - verifyStartMs
                            if (result.data.valid) {
                                VectorCamAnalytics.logEvent(
                                    "registration_access_code_validated",
                                    mapOf(
                                        "program_id" to selectedProgram.id,
                                        "duration_ms" to verifyDurationMs
                                    )
                                )
                                _state.update {
                                    it.copy(
                                        isProgramAccessCodeDialogVisible = false,
                                        programAccessCodeInput = "",
                                        programAccessCodeError = null
                                    )
                                }
                                registerCollectorAndProceed(selectedProgram)
                            } else {
                                VectorCamAnalytics.logEvent(
                                    "registration_access_code_rejected",
                                    mapOf("program_id" to selectedProgram.id)
                                )
                                _state.update {
                                    it.copy(
                                        programAccessCodeError = RegistrationError.INVALID_PROGRAM_ACCESS_CODE,
                                        isLoading = false
                                    )
                                }
                            }
                        }
                        is Result.Error -> {
                            emitError(result.error)
                            _state.update { it.copy(isLoading = false) }
                        }
                    }
                }
            }
        }
    }

    private fun validateCollectorInputs(): Boolean {
        val collector = _state.value.collector
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

        return listOf(
            collectorNameValidationResult,
            collectorTitleValidationResult,
            collectorLastTrainedOnValidationResult
        ).none { it is Result.Error }
    }

    private suspend fun registerCollectorAndProceed(selectedProgram: Program) {
        if (!_isConnectedToInternet.value) {
            emitError(NetworkError.NO_INTERNET)
            return
        }

        val seedingStartMs = System.currentTimeMillis()
        VectorCamAnalytics.logEvent(
            "registration_seeding_started",
            mapOf("program_id" to selectedProgram.id)
        )

        try {
            _state.update { it.copy(isLoading = true) }
            transactionHelper.runAsTransaction {
                fetchAndSeedAllLocationTypesForProgram(selectedProgram.id)
                fetchAndSeedAllSitesForProgram(selectedProgram.id)
                fetchAndSeedFormForProgram(selectedProgram.id)
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

            // Wire user identity for Crashlytics + GA4 cohort analysis
            VectorCamAnalytics.setDevice(device, selectedProgram.id)
            Crashy.setDevice(device)

            val seedingDurationMs = System.currentTimeMillis() - seedingStartMs
            VectorCamAnalytics.logEvent(
                "registration_seeding_succeeded",
                mapOf("program_id" to selectedProgram.id, "duration_ms" to seedingDurationMs)
            )
            VectorCamAnalytics.logEvent(
                "registration_completed",
                mapOf("program_id" to selectedProgram.id)
            )
            VectorCamAnalytics.logEvent(
                "device_registered",
                mapOf("program_id" to selectedProgram.id)
            )

            _events.send(RegistrationEvent.NavigateToLandingScreen)
        } catch (e: Exception) {
            emitError(RegistrationError.UNKNOWN_ERROR)
            RegistrationErrorLogger.logDeviceRegistrationFailure(e, selectedProgram.id)
            VectorCamAnalytics.logEvent(
                "registration_seeding_failed",
                mapOf(
                    "program_id" to selectedProgram.id,
                    "error_class" to "REGISTRATION_SEEDING",
                    "error_message" to (e.message?.take(100) ?: "unknown")
                )
            )
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun loadRegistrationDetails() {
        viewModelScope.launch {
            fetchAndSeedAllPrograms()
        }
    }

    private suspend fun fetchAndSeedAllPrograms() {
        _state.update { it.copy(isLoadingPrograms = true) }
        val fetchStartMs = System.currentTimeMillis()
        try {
            when (val result = programDataSource.getAllPrograms()) {
                is Result.Success -> {
                    transactionHelper.runAsTransaction {
                        result.data.programs.forEach { programDto ->
                            programRepository.upsertProgram(programDto.toDomain())
                        }
                    }
                    VectorCamAnalytics.logEvent(
                        "registration_programs_fetched",
                        mapOf(
                            "program_count" to result.data.programs.size,
                            "duration_ms" to (System.currentTimeMillis() - fetchStartMs)
                        )
                    )
                }

                is Result.Error -> {
                    VectorCamAnalytics.logEvent(
                        "registration_programs_fetch_failed",
                        mapOf(
                            "error_class" to (result.error as? Enum<*>)?.name,
                            "duration_ms" to (System.currentTimeMillis() - fetchStartMs)
                        )
                    )
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
                result.data.locationTypes.forEach { locationTypeDto ->
                    locationTypeRepository.upsertLocationType(
                        locationTypeDto.toDomain(),
                        programId
                    ).onError { error ->
                        emitError(error)
                        throw Exception("Failed to save location types for program $programId")
                    }
                }
            }

            is Result.Error -> {
                emitError(result.error)
                throw Exception("Failed to fetch location types for program $programId")
            }
        }
    }

    private suspend fun fetchAndSeedAllSitesForProgram(programId: Int) {
        when (val result = siteDataSource.getAllSitesForProgram(programId)) {
            is Result.Success -> {
                result.data.sortByHierarchy().forEach { siteDto ->
                    val locationTypeId = siteDto.locationTypeId
                    val parentId = siteDto.parentId

                    siteRepository.upsertSite(
                        siteDto.toDomain(),
                        programId,
                        locationTypeId,
                        parentId
                    ).onError { error ->
                        emitError(error)
                        throw Exception("Failed to save sites for program $programId")
                    }
                }
            }

            is Result.Error -> {
                emitError(result.error)
                throw Exception("Failed to fetch sites for program $programId")
            }
        }
    }

    private suspend fun fetchAndSeedFormForProgram(programId: Int) {
        when (val result = formDataSource.getCurrentFormByProgramId(programId)) {
            is Result.Success -> {
                val formDto = result.data
                formRepository.upsertForm(formDto.toDomain(), programId).onError { error ->
                    emitError(error)
                    throw Exception("Failed to save surveillance form for program $programId")
                }
                seedFormQuestionsForProgram(formDto.questions, formDto.id, null)
                val currentProgram = programRepository.getProgramById(programId)
                if (currentProgram != null) {
                    programRepository.upsertProgram(currentProgram.copy(formVersion = formDto.version))
                }
            }

            is Result.Error -> {
                if (result.error != NetworkError.NOT_FOUND) {
                    emitError(result.error)
                    throw Exception("Failed to fetch surveillance form for program $programId")
                }
            }
        }
    }

    private suspend fun seedFormQuestionsForProgram(
        questionDtos: List<FormQuestionDto>,
        formId: Int,
        parentId: Int?
    ) {
        questionDtos.forEach { questionDto ->
            formQuestionRepository.upsertFormQuestion(questionDto.toDomain(), formId, parentId).onError { error ->
                emitError(error)
                throw Exception("Failed to save surveillance form questions")
            }
            questionDto.subQuestions?.let { subQuestions ->
                seedFormQuestionsForProgram(subQuestions, formId, questionDto.id)
            }
        }
    }
}
