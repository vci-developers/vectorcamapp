package com.vci.vectorcamapp.intake.presentation
import com.vci.vectorcamapp.program.kenya.domain.KenyaFormRules
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions

import androidx.lifecycle.SavedStateHandle
import android.location.Location
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DefaultIntakeFieldsCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormAnswerValue
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
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.success.SuccessMessage
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
) : CoreViewModel() {

    companion object {
        private const val LOCATION_TIMEOUT_MS = 30000L
    }
    // Live verification is separate from the session's original collection location.
    private var verificationLocation: Location? = null

    @Inject
    lateinit var transactionHelper: TransactionHelper
    @Inject
    lateinit var workManagerRepository: WorkManagerRepository
    @Inject
    lateinit var programFormWorkflowFactory: ProgramFormWorkflowFactory
    @Inject
    lateinit var collectionMethodWorkflowFactory: CollectionMethodWorkflowFactory

    private lateinit var programFormWorkflow: ProgramFormWorkflow

    private lateinit var collectionMethodWorkflow: CollectionMethodWorkflow

    private val _allCollectors = collectorRepository.observeAllCollectors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _state = MutableStateFlow(IntakeState())
    val state = combine(_allCollectors, _state) { allCollectors, state ->
        state.copy(allCollectors = allCollectors)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        IntakeState()
    )

    private val _events = Channel<IntakeEvent>()
    val events = _events.receiveAsFlow()
    init {
        loadFormDetails()
    }

    fun onAction(action: IntakeAction) {
        viewModelScope.launch {
            if (_state.value.isSubmitting) return@launch
            // A saved session's collection site and coordinates are historical, not the imaging location.
            if (_state.value.isResumingSession && when (action) {
                    IntakeAction.RetryLocation, is IntakeAction.SelectDistrict,
                    is IntakeAction.SelectVillageName, is IntakeAction.SelectHouseNumber,
                    is IntakeAction.SelectLocationTypeSiteOption -> true
                    else -> false
                }) return@launch
            val requiresSiteProximity = when (action) {
                IntakeAction.SubmitIntakeForm,
                is IntakeAction.UpdateFormAnswer,
                is IntakeAction.EnterNumPeopleSleptInHouse,
                is IntakeAction.ToggleIrsConducted,
                is IntakeAction.EnterMonthsSinceIrs,
                is IntakeAction.EnterNumLlinsAvailable,
                is IntakeAction.SelectLlinType,
                is IntakeAction.SelectLlinBrand,
                is IntakeAction.EnterNumPeopleSleptUnderLlin,
                is IntakeAction.EnterNumChildrenUnder5,
                is IntakeAction.ToggleHasPregnantWoman -> true
                else -> false
            }
            if (requiresSiteProximity && !_state.value.canFillForm) {
                emitError(IntakeError.SITE_PROXIMITY_REQUIRED)
                return@launch
            }
            when (action) {
                IntakeAction.ReturnToPreviousScreen -> {
                    currentSessionCache.clearSession()
                    _events.send(IntakeEvent.NavigateBackToPreviousScreen)
                }

                IntakeAction.SubmitIntakeForm, IntakeAction.ConfirmSubmitWithoutConsent -> {
                    val isRefusal = action == IntakeAction.ConfirmSubmitWithoutConsent
                    if (if (isRefusal) !_state.value.hasDeclinedConsent else !_state.value.hasHouseholdHeadConsent) {
                        emitError(IntakeError.CONSENT_REQUIRED)
                        return@launch
                    }
                    _state.update { it.copy(isSubmitting = true) }
                    try {
                        getLocation()
                        if (!_state.value.canFillForm) {
                            emitError(IntakeError.SITE_PROXIMITY_REQUIRED)
                            return@launch
                        }
                        // Consent may have changed while the GPS request was in flight.
                        if (if (isRefusal) !_state.value.hasDeclinedConsent else !_state.value.hasHouseholdHeadConsent) {
                            emitError(IntakeError.CONSENT_REQUIRED)
                            return@launch
                        }
                        val isNewSession =
                            sessionRepository.getSessionById(_state.value.session.localId) == null
                        IntakeSubmission(
                            validation = intakeValidationUseCases,
                            transactions = transactionHelper,
                            sessions = sessionRepository,
                            surveillanceForms = surveillanceFormRepository,
                            formAnswers = formAnswerRepository,
                            sessionCache = currentSessionCache,
                            defaultFields = defaultIntakeFieldsCache,
                            workManager = workManagerRepository,
                        ).submit(
                            currentState = { _state.value },
                            isCurrentCollectorMissing = { state.value.isCurrentCollectorMissing },
                            updateState = { transform -> _state.update(transform) },
                            reportError = { emitError(it) },
                            isRefusal = isRefusal,
                            onSubmitted = { session ->
                                collectionMethodWorkflow = collectionMethodWorkflowFactory.create(
                                    session.localId,
                                    session.collectionMethod,
                                )
                                if (isNewSession) {
                                    emitSuccess(SuccessMessage.SESSION_CREATED)
                                }
                                _events.send(
                                    if (_state.value.isResumingSession && !isRefusal) {
                                        IntakeEvent.NavigateAfterIntake(collectionMethodWorkflow.postIntakeDestination)
                                    } else {
                                        IntakeEvent.NavigateHomeAfterCollectionSaved
                                    }
                                )
                            },
                        )
                    } finally {
                        _state.update { it.copy(isSubmitting = false) }
                    }
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
                        ).withSiteProximity()
                    }
                }

                is IntakeAction.SelectDistrict -> {
                    _state.update {
                        it.copy(
                            selectedDistrict = action.district,
                            selectedVillageName = "",
                            selectedHouseNumber = "",
                            selectedSiteWithCoordinates = null
                        ).withSiteProximity()
                    }
                }

                is IntakeAction.SelectVillageName -> {
                    _state.update {
                        it.copy(
                            selectedVillageName = action.villageName,
                            selectedHouseNumber = "",
                            selectedSiteWithCoordinates = null

                        ).withSiteProximity()
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
                        ).withSiteProximity()
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
                    val question = _state.value.formQuestions.find { it.id == action.questionId }
                    if (question != null && KenyaFormRules.isHouseholdNumber(question)) {
                        val number = (action.value as? FormAnswerValue.StringValue)?.value ?: return@launch
                        if (number.length > 2 || number.any { it !in '0'..'9' }) return@launch
                    }
                    if (question != null && !KenyaFormRules.isWithinNumberLimit(question, action.value)) {
                        return@launch
                    }
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
                                    val defaultValue = FormAnswerValue.emptyFor(question.type)
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
                    if (_state.value.isLoading || _state.value.isCheckingLocation) return@launch
                    _state.update {
                        it.copy(
                            locationError = null
                        )
                    }
                    getLocation()
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
                collectionMethod = IntakeDropdownOptions.CollectionMethodOption.PROKOPACK_ASPIRATOR.label,
                specimenCondition = IntakeDropdownOptions.SpecimenConditionOption.FRESH.label,
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
            ) { allSites, currentSavedSurveillanceForm, currentAllCollectors, currentAllLocationTypes ->
                // A resumed session's locked site determines its location structure.
                // Unrelated historical sites must not switch the UI or save path to legacy mode.
                val currentAllSites = if (currentSession == null) {
                    allSites.filter { it.isActive }
                } else {
                    allSites.filter { it.id == currentSessionSiteId }
                }

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
                        isResumingSession = currentSession != null,
                        canUpdateLocation = currentSession == null,
                        surveillanceForm = currentSavedSurveillanceForm
                            ?: programFormWorkflow.surveillanceForm,
                        form = programFormWorkflow.form,
                        formQuestions = programFormWorkflow.sessionFormQuestions,
                        formAnswersByQuestionId = programFormWorkflow.sessionFormQuestions.associate { question ->
                            question.id to (
                                savedFormAnswers[question.id] ?: FormAnswer(
                                localId = UUID.randomUUID(),
                                remoteId = null,
                                value = if (currentSession == null && KenyaFormRules.isConsentQuestion(question)) {
                                    FormAnswerValue.StringValue("true")
                                } else FormAnswerValue.emptyFor(question.type),
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
                    ).withSiteProximity()
                }
            }.first()

            getLocation()
        }
    }

    private suspend fun getLocation() {
        if (_state.value.isResumingSession || _state.value.isCheckingLocation) return
        verificationLocation = null
        _state.update { it.copy(isCheckingLocation = true).withSiteProximity() }
        try {
            val locationResult = try {
                withTimeout(LOCATION_TIMEOUT_MS) {
                    locationRepository.getCurrentLocation()
                }
            } catch (e: SecurityException) {
                Result.Error(IntakeError.LOCATION_PERMISSION_DENIED)
            } catch (e: TimeoutCancellationException) {
                Result.Error(IntakeError.LOCATION_GPS_TIMEOUT)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                IntakeErrorLogger.logLocationFetchFailed(e)
                Result.Error(IntakeError.UNKNOWN_ERROR)
            }

            locationResult.onSuccess { location ->
                verificationLocation = location
                _state.update {
                    it.copy(
                        session = if (it.canUpdateLocation) it.session.copy(
                            latitude = location.latitude.toFloat(),
                            longitude = location.longitude.toFloat(),
                            gpsAccuracy = location.accuracy,
                        ) else it.session,
                        locationError = null
                    ).withSiteProximity()
                }
            }.onError { error ->
                _state.update { it.copy(locationError = error) }
                if (error != IntakeError.LOCATION_GPS_TIMEOUT) {
                    emitError(error)
                }
            }
        } finally {
            _state.update { it.copy(isCheckingLocation = false) }
        }
    }
    private fun IntakeState.withSiteProximity(): IntakeState {
        val location = verificationLocation
        val collectionLatitude = if (isResumingSession) session.latitude?.toDouble() else location?.latitude
        val collectionLongitude = if (isResumingSession) session.longitude?.toDouble() else location?.longitude
        val latitude = selectedSiteWithCoordinates?.latitude?.toDouble()
        val longitude = selectedSiteWithCoordinates?.longitude?.toDouble()
        val distance = if (
            collectionLatitude != null && collectionLongitude != null && latitude != null && longitude != null &&
            collectionLatitude.isFinite() && collectionLatitude in -90.0..90.0 &&
            collectionLongitude.isFinite() && collectionLongitude in -180.0..180.0 &&
            latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude.isFinite() && longitude in -180.0..180.0
        ) {
            val results = FloatArray(1)
            Location.distanceBetween(collectionLatitude, collectionLongitude, latitude, longitude, results)
            results[0].takeIf { it.isFinite() && it >= 0f }
        } else null
        return copy(
            distanceToSiteMeters = distance,
            isWithinSiteRadius = KenyaFormRules.isWithinSiteRadius(distance)
        )
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
