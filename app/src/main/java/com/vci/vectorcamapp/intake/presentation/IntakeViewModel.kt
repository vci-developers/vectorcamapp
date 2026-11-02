package com.vci.vectorcamapp.intake.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DefaultIntakeFieldsCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.SurveillanceForm
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.domain.repository.CollectorRepository
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.LocationTypeRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.repository.SurveillanceFormRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.intake.domain.repository.LocationRepository
import com.vci.vectorcamapp.intake.domain.strategy.collection_method.CollectionMethodWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.collection_method.CollectionMethodWorkflowFactory
import com.vci.vectorcamapp.intake.domain.strategy.program_form.ProgramFormWorkflow
import com.vci.vectorcamapp.intake.domain.strategy.program_form.ProgramFormWorkflowFactory
import com.vci.vectorcamapp.intake.domain.use_cases.IntakeValidationUseCases
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
import com.vci.vectorcamapp.intake.domain.util.IntakeError
import com.vci.vectorcamapp.intake.domain.util.SiteSelectionResolver
import com.vci.vectorcamapp.intake.logging.IntakeErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class IntakeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val intakeValidationUseCases: IntakeValidationUseCases,
    private val deviceCache: DeviceCache,
    private val currentSessionCache: CurrentSessionCache,
    private val defaultIntakeFieldsCache: DefaultIntakeFieldsCache,
    private val siteRepository: SiteRepository,
    private val locationTypeRepository: LocationTypeRepository,
    private val surveillanceFormRepository: SurveillanceFormRepository,
    private val sessionRepository: SessionRepository,
    private val sessionUnitRepository: SessionUnitRepository,
    private val locationRepository: LocationRepository,
    private val collectorRepository: CollectorRepository,
    private val programRepository: ProgramRepository,
    private val formAnswerRepository: FormAnswerRepository,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

    companion object {
        private const val LOCATION_TIMEOUT_MS = 30000L
    }

    @Inject
    lateinit var transactionHelper: TransactionHelper

    @Inject
    lateinit var programFormWorkflowFactory: ProgramFormWorkflowFactory
    private lateinit var programFormWorkflow: ProgramFormWorkflow

    @Inject
    lateinit var collectionMethodWorkflowFactory: CollectionMethodWorkflowFactory
    private lateinit var collectionMethodWorkflow: CollectionMethodWorkflow

    private val _allCollectors = collectorRepository.observeAllCollectors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _state = MutableStateFlow(IntakeState())
    val state = combine(_allCollectors, _state) { allCollectors, state ->
        state.copy(allCollectors = allCollectors)
    }.onStart {
        loadFormDetails()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        IntakeState()
    )

    private val _events = Channel<IntakeEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: IntakeAction) {
        viewModelScope.launch {
            when (action) {
                IntakeAction.ReturnToPreviousScreen -> {
                    currentSessionCache.clearSession()
                    _events.send(IntakeEvent.NavigateBackToPreviousScreen)
                }

                IntakeAction.SubmitIntakeForm -> {
                    IntakeSubmission(
                        validation = intakeValidationUseCases,
                        transactions = transactionHelper,
                        sessions = sessionRepository,
                        surveillanceForms = surveillanceFormRepository,
                        formAnswers = formAnswerRepository,
                        sessionCache = currentSessionCache,
                        defaultFields = defaultIntakeFieldsCache,
                    ).submit(
                        currentState = { _state.value },
                        isCurrentCollectorMissing = { state.value.isCurrentCollectorMissing },
                        updateState = { transform -> _state.update(transform) },
                        reportError = { emitError(it) },
                        onSubmitted = { session ->
                            collectionMethodWorkflow = collectionMethodWorkflowFactory.create(
                                session.localId,
                                session.collectionMethod,
                            )
                            _events.send(
                                IntakeEvent.NavigateAfterIntake(collectionMethodWorkflow.postIntakeDestination),
                            )
                        },
                    )
                }

                is IntakeAction.SelectCollector -> {
                    val updatedCollector = action.collector

                    _state.update {
                        it.copy(
                            session = it.session.copy(
                                collectorName = updatedCollector.name,
                                collectorTitle = updatedCollector.title,
                                collectorLastTrainedOn = updatedCollector.lastTrainedOn
                            ),
                            isCurrentCollectorMissing = false
                        )
                    }
                }

                is IntakeAction.EnterHardwareId -> {
                    _state.update {
                        it.copy(
                            session = it.session.copy(
                                hardwareId = action.text
                            )
                        )
                    }
                }

                is IntakeAction.SelectLocationTypeSiteOption -> {
                    val locationTypeId = action.locationTypeId
                    val selectedOption = action.selectedOption

                    val allLocationTypes = _state.value.allLocationTypesInProgram

                    val selectedIndex = allLocationTypes.indexOfFirst { it.id == locationTypeId }
                    if (selectedIndex == -1) return@launch

                    val updatedSiteSelections =
                        _state.value.siteSelectionsByLocationTypeId.toMutableMap().apply {
                            put(locationTypeId, selectedOption)

                            val downstreamLocationTypeIds =
                                allLocationTypes.drop(selectedIndex + 1).map { it.id }
                            downstreamLocationTypeIds.forEach { remove(it) }
                        }

                    _state.update {
                        it.copy(
                            siteSelectionsByLocationTypeId = updatedSiteSelections,
                            selectedSiteWithCoordinates = SiteSelectionResolver.findSelectedSiteWithCoordinates(
                                allSites = it.allSitesInProgram,
                                allLocationTypes = it.allLocationTypesInProgram,
                                siteSelectionsByLocationTypeId = updatedSiteSelections
                            )
                        )
                    }
                }

                is IntakeAction.SelectDistrict -> {
                    _state.update {
                        it.copy(
                            selectedDistrict = action.district,
                            selectedVillageName = "",
                            selectedHouseNumber = "",
                            selectedSiteWithCoordinates = null
                        )
                    }
                }

                is IntakeAction.SelectVillageName -> {
                    _state.update {
                        it.copy(
                            selectedVillageName = action.villageName,
                            selectedHouseNumber = "",
                            selectedSiteWithCoordinates = null

                        )
                    }
                }

                is IntakeAction.SelectHouseNumber -> {
                    _state.update {
                        it.copy(
                            selectedHouseNumber = action.houseNumber,
                            selectedSiteWithCoordinates = SiteSelectionResolver.findSelectedSiteWithCoordinates(
                                allSites = it.allSitesInProgram,
                                allLocationTypes = it.allLocationTypesInProgram,
                                siteSelectionsByLocationTypeId = it.siteSelectionsByLocationTypeId,
                                selectedDistrict = it.selectedDistrict,
                                selectedVillageName = it.selectedVillageName,
                                selectedHouseNumber = action.houseNumber
                            )
                        )
                    }
                }

                is IntakeAction.EnterNumPeopleSleptInHouse -> {
                    val oldValue = _state.value.surveillanceForm?.numPeopleSleptInHouse
                    val normalized = normalizeNumericInput(oldValue, action.count)
                    val numPeopleSleptInHouse = normalized.toIntOrNull() ?: -1
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm?.copy(
                                numPeopleSleptInHouse = numPeopleSleptInHouse
                            )
                        )
                    }
                }

                is IntakeAction.ToggleIrsConducted -> {
                    val wasIrsConducted = action.isChecked
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm?.copy(
                                wasIrsConducted = wasIrsConducted,
                                monthsSinceIrs = if (wasIrsConducted) -1 else null
                            )
                        )
                    }
                }

                is IntakeAction.EnterMonthsSinceIrs -> {
                    val oldValue = _state.value.surveillanceForm?.monthsSinceIrs
                    val normalized = normalizeNumericInput(oldValue, action.count)
                    val monthsSinceIrs = normalized.toIntOrNull() ?: -1
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm?.copy(
                                monthsSinceIrs = monthsSinceIrs
                            )
                        )
                    }
                }

                is IntakeAction.EnterNumLlinsAvailable -> {
                    val oldValue = _state.value.surveillanceForm?.numLlinsAvailable
                    val normalized = normalizeNumericInput(oldValue, action.count)
                    val numLlinsAvailable = normalized.toIntOrNull() ?: -1
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm?.copy(
                                numLlinsAvailable = numLlinsAvailable
                            )
                        )
                    }
                    if (numLlinsAvailable <= 0) {
                        _state.update {
                            it.copy(
                                surveillanceForm = it.surveillanceForm?.copy(
                                    llinType = null,
                                    llinBrand = null,
                                    numPeopleSleptUnderLlin = null
                                )
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                surveillanceForm = it.surveillanceForm?.copy(
                                    llinType = "",
                                    numPeopleSleptUnderLlin = -1
                                )
                            )
                        }
                    }
                }

                is IntakeAction.SelectLlinType -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm?.copy(
                                llinType = action.option.label,
                                llinBrand = ""
                            )
                        )
                    }
                }

                is IntakeAction.SelectLlinBrand -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm?.copy(
                                llinBrand = action.option.label
                            )
                        )
                    }
                }

                is IntakeAction.EnterNumPeopleSleptUnderLlin -> {
                    val oldValue = _state.value.surveillanceForm?.numPeopleSleptUnderLlin
                    val normalized = normalizeNumericInput(oldValue, action.count)
                    val numPeopleSleptUnderLlin = normalized.toIntOrNull() ?: -1
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm?.copy(
                                numPeopleSleptUnderLlin = numPeopleSleptUnderLlin
                            )
                        )
                    }
                }

                is IntakeAction.EnterNumChildrenUnder5 -> {
                    val oldValue = _state.value.surveillanceForm?.numChildrenUnder5
                    val normalized = normalizeNumericInput(oldValue, action.count)
                    val numChildrenUnder5 = normalized.toIntOrNull()
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm?.copy(
                                numChildrenUnder5 = numChildrenUnder5
                            )
                        )
                    }
                }

                is IntakeAction.ToggleHasPregnantWoman -> {
                    val hasPregnantWoman = action.isChecked
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm?.copy(
                                hasPregnantWoman = hasPregnantWoman
                            )
                        )
                    }
                }

                is IntakeAction.PickCollectionDate -> {
                    _state.update {
                        it.copy(
                            session = it.session.copy(
                                collectionDate = action.date
                            )
                        )
                    }
                }

                is IntakeAction.UpdateCollectionMethod -> {
                    if (_state.value.isCollectionMethodLocked) return@launch
                    _state.update {
                        it.copy(
                            session = it.session.copy(
                                collectionMethod = action.collectionMethod
                            )
                        )
                    }
                }

                is IntakeAction.UpdateSpecimenCondition -> {
                    _state.update {
                        it.copy(
                            session = it.session.copy(
                                specimenCondition = action.specimenCondition
                            )
                        )
                    }
                }

                is IntakeAction.UpdateFormAnswer -> {
                    _state.update {
                        val updatedAnswers = it.formAnswersByQuestionId.toMutableMap().apply {
                            val existingFormAnswer = get(action.questionId)
                            if (existingFormAnswer != null) {
                                put(
                                    action.questionId,
                                    existingFormAnswer.copy(value = action.value)
                                )
                            }
                        }

                        val answerMap =
                            updatedAnswers.mapValues { (_, answer) -> answer.value }.toMutableMap()

                        it.formQuestions.forEach { question ->
                            if (question.id != action.questionId && !FormQuestionPrerequisiteEvaluator.evaluate(
                                    question.prerequisite, answerMap
                                )
                            ) {
                                updatedAnswers[question.id]?.let { answer ->
                                    val defaultValue = when (question.type) {
                                        "boolean" -> "false"
                                        else -> ""
                                    }
                                    updatedAnswers[question.id] = answer.copy(value = defaultValue)
                                    answerMap[question.id] = defaultValue
                                }
                            }
                        }

                        it.copy(formAnswersByQuestionId = updatedAnswers)
                    }
                }

                is IntakeAction.EnterNotes -> {
                    _state.update {
                        it.copy(
                            session = it.session.copy(
                                notes = action.text
                            )
                        )
                    }
                }

                is IntakeAction.RetryLocation -> {
                    if (_state.value.isLoading || !_state.value.canUpdateLocation) return@launch
                    _state.update {
                        it.copy(
                            session = it.session.copy(latitude = null, longitude = null, gpsAccuracy = null),
                            locationError = null
                        )
                    }
                    getLocation()
                }

                is IntakeAction.ShowCollectionMethodTooltipDialog -> {
                    _state.update { it.copy(isCollectionMethodTooltipVisible = true) }
                }

                is IntakeAction.HideCollectionMethodTooltipDialog -> {
                    _state.update { it.copy(isCollectionMethodTooltipVisible = false) }
                }

                is IntakeAction.RegisterMissingCollector -> {
                    val name = _state.value.session.collectorName
                    val title = _state.value.session.collectorTitle
                    val lastTrainedOn = _state.value.session.collectorLastTrainedOn

                    if (name.isBlank() || title.isBlank()) {
                        return@launch
                    }

                    collectorRepository.upsertCollector(
                        Collector(
                            id = UUID.randomUUID(),
                            name = name,
                            title = title,
                            lastTrainedOn = lastTrainedOn
                        )
                    ).onSuccess {
                        _state.update {
                            it.copy(
                                isCurrentCollectorMissing = false
                            )
                        }
                    }.onError {
                        emitError(IntakeError.COLLECTOR_SAVE_FAILED)
                    }
                }
            }
        }
    }

    private fun loadFormDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val programId = deviceCache.getProgramId()
            if (programId == null) {
                emitError(IntakeError.PROGRAM_NOT_FOUND)
                IntakeErrorLogger.logProgramNotFound(Exception(IntakeError.PROGRAM_NOT_FOUND.name))
                _events.send(IntakeEvent.NavigateBackToRegistrationScreen)
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val program = programRepository.getProgramById(programId)
            if (program == null) {
                emitError(IntakeError.PROGRAM_NOT_FOUND)
                IntakeErrorLogger.logProgramNotFound(Exception(IntakeError.PROGRAM_NOT_FOUND.name))
                _events.send(IntakeEvent.NavigateBackToRegistrationScreen)
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            val currentSession = currentSessionCache.getSession()
            val resolvedSessionType =
                currentSession?.type ?: savedStateHandle.get<SessionType>("sessionType")
                ?: SessionType.SURVEILLANCE

            programFormWorkflow = programFormWorkflowFactory.create(resolvedSessionType, program)

            val defaultFields = defaultIntakeFieldsCache.getDefaultIntakeFields()
            val cachedCollectorName = defaultFields?.collectorName.orEmpty()
            val cachedCollectorTitle = defaultFields?.collectorTitle.orEmpty()
            val cachedCollectorLastTrainedOn = defaultFields?.collectorLastTrainedOn ?: 0L
            val cachedDefaultHardwareId = defaultFields?.hardwareId.orEmpty()
            val cachedDefaultDistrict = defaultFields?.district.orEmpty()
            val cachedDefaultVillageName = defaultFields?.villageName.orEmpty()
            val cachedLocationSelection = defaultFields?.locationSelections ?: emptyMap()

            val effectiveSession = currentSession ?: _state.value.session.copy(
                type = resolvedSessionType,
                hardwareId = cachedDefaultHardwareId,
                collectorName = cachedCollectorName,
                collectorTitle = cachedCollectorTitle,
                collectorLastTrainedOn = cachedCollectorLastTrainedOn
            )

            val currentSessionSiteId = currentSessionCache.getSiteId()

            val savedFormAnswers = currentSession?.let {
                formAnswerRepository.getSessionScopedFormAnswers(it.localId)
            } ?: emptyMap()

            val isCollectionMethodLocked = currentSession?.let {
                sessionUnitRepository.countSessionUnitsForSession(it.localId) > 0
            } ?: false

            combine(
                siteRepository.observeAllSitesByProgramId(programId),
                currentSession?.let {
                    surveillanceFormRepository.observeSurveillanceFormBySessionId(it.localId)
                } ?: flowOf<SurveillanceForm?>(null),
                collectorRepository.observeAllCollectors(),
                locationTypeRepository.observeAllLocationTypesByProgramId(programId)
            ) { currentAllSites, currentSavedSurveillanceForm, currentAllCollectors, currentAllLocationTypes ->

                val validatedSite = currentAllSites.find { it.id == currentSessionSiteId }
                val validatedSiteSelectionsByLocationTypeId = when {
                    validatedSite == null -> {
                        cachedLocationSelection.filterKeys { cachedId ->
                            currentAllLocationTypes.any { it.id == cachedId }
                        }
                    }

                    validatedSite.locationHierarchy?.isNotEmpty() == true -> {
                        currentAllLocationTypes.mapNotNull { locationType ->
                            validatedSite.locationHierarchy[locationType.name]?.let { selectedLocationTypeSite ->
                                locationType.id to selectedLocationTypeSite
                            }
                        }.toMap()
                    }

                    else -> {
                        emptyMap()
                    }
                }

                val validatedDistrict = when {
                    validatedSite != null -> validatedSite.district.orEmpty()
                    currentAllSites.any { it.district == cachedDefaultDistrict } -> cachedDefaultDistrict
                    else -> ""
                }

                val validatedVillageName = when {
                    validatedSite != null -> validatedSite.villageName.orEmpty()
                    validatedDistrict.isNotBlank() && currentAllSites.any {
                        it.district == validatedDistrict && it.villageName == cachedDefaultVillageName
                    } -> cachedDefaultVillageName

                    else -> ""
                }

                val validatedHouseNumber = validatedSite?.houseNumber.orEmpty()

                val sessionCollectorName = effectiveSession.collectorName
                val sessionCollectorTitle = effectiveSession.collectorTitle

                val hasMatchingCollector = currentAllCollectors.any { collector ->
                    collector.name == sessionCollectorName && collector.title == sessionCollectorTitle
                }

                val isCollectorMissing =
                    sessionCollectorName.isNotBlank() && sessionCollectorTitle.isNotBlank() && !hasMatchingCollector

                _state.update {
                    it.copy(
                        isLoading = false,
                        session = effectiveSession,
                        canUpdateLocation = effectiveSession.latitude == null || effectiveSession.longitude == null,
                        surveillanceForm = currentSavedSurveillanceForm
                            ?: programFormWorkflow.surveillanceForm,
                        form = programFormWorkflow.form,
                        formQuestions = programFormWorkflow.sessionFormQuestions,
                        formAnswersByQuestionId = programFormWorkflow.sessionFormQuestions.associate { question ->
                            question.id to (
                                savedFormAnswers[question.id] ?: FormAnswer(
                                localId = UUID.randomUUID(),
                                remoteId = null,
                                value = when (question.type) {
                                    "boolean" -> "false"
                                    else -> ""
                                },
                                dataType = question.type,
                                submittedAt = 0L
                            )
                            )
                        },
                        siteSelectionsByLocationTypeId = validatedSiteSelectionsByLocationTypeId,
                        allSitesInProgram = currentAllSites,
                        selectedSiteWithCoordinates = SiteSelectionResolver.findSelectedSiteWithCoordinates(
                            allSites = currentAllSites,
                            allLocationTypes = currentAllLocationTypes,
                            siteSelectionsByLocationTypeId = validatedSiteSelectionsByLocationTypeId,
                            selectedDistrict = validatedDistrict,
                            selectedVillageName = validatedVillageName,
                            selectedHouseNumber = validatedHouseNumber
                        ),
                        allCollectors = currentAllCollectors,
                        allLocationTypesInProgram = currentAllLocationTypes,
                        selectedDistrict = validatedDistrict,
                        selectedVillageName = validatedVillageName,
                        selectedHouseNumber = validatedHouseNumber,
                        isCurrentCollectorMissing = isCollectorMissing,
                        isCollectionMethodLocked = isCollectionMethodLocked,
                    )
                }
            }.first()

            getLocation()
        }
    }

    private suspend fun getLocation() {
        if (!_state.value.canUpdateLocation) return
        if (_state.value.session.latitude == null || _state.value.session.longitude == null) {
            val locationResult = try {
                withTimeout(LOCATION_TIMEOUT_MS) {
                    locationRepository.getCurrentLocation()
                }
            } catch (e: SecurityException) {
                Result.Error(IntakeError.LOCATION_PERMISSION_DENIED)
            } catch (e: TimeoutCancellationException) {
                Result.Error(IntakeError.LOCATION_GPS_TIMEOUT)
            } catch (e: Exception) {
                IntakeErrorLogger.logLocationFetchFailed(e)
                Result.Error(IntakeError.UNKNOWN_ERROR)
            }

            locationResult.onSuccess { location ->
                _state.update {
                    it.copy(
                        session = it.session.copy(
                            latitude = location.latitude.toFloat(),
                            longitude = location.longitude.toFloat(),
                            gpsAccuracy = location.accuracy,
                        ),
                        locationError = null
                    )
                }
            }.onError { error ->
                _state.update { it.copy(locationError = error) }
                if (error != IntakeError.LOCATION_GPS_TIMEOUT) {
                    emitError(error)
                }
            }
        }
    }

    private fun normalizeNumericInput(oldValue: Int?, newValue: String): String {
        val oldValueString = (oldValue ?: 0).toString()
        val filteredNewValue = newValue.filter { it.isDigit() }

        val finalValueString = if (oldValueString == "0" && filteredNewValue.length > 1) {
            filteredNewValue.filter { it != '0' }
        } else {
            filteredNewValue
        }

        return finalValueString.toIntOrNull().toString()
    }
}
