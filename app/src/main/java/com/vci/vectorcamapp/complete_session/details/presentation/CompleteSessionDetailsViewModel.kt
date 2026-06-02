package com.vci.vectorcamapp.complete_session.details.presentation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.collection_batch.domain.util.CollectionBatchIdentityResolver
import com.vci.vectorcamapp.complete_session.details.domain.util.CompleteSessionDetailsError
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.composites.FormAnswerAndQuestion
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.presentation.util.search.SearchUtils
import com.vci.vectorcamapp.ui.extensions.displayText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/* TODO: CLEANUP */
@HiltViewModel
class CompleteSessionDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val specimenRepository: SpecimenRepository,
    private val sessionUnitRepository: SessionUnitRepository,
    private val formAnswerRepository: FormAnswerRepository,
    private val formQuestionRepository: FormQuestionRepository,
    private val formRepository: FormRepository,
    private val programRepository: ProgramRepository,
    private val deviceCache: DeviceCache,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

    private val sessionId: UUID? =
        savedStateHandle.get<String>("sessionId")
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private val _specimensWithImagesAndInferenceResults: Flow<List<SpecimenWithSpecimenImagesAndInferenceResults>> =
        if (sessionId == null) flowOf(emptyList())
        else specimenRepository.observeSpecimenImagesAndInferenceResultsBySessionScope(
            sessionId,
            null
        )

    private val _sessionUnitIdBySpecimenId: Flow<Map<String, UUID?>> =
        _specimensWithImagesAndInferenceResults.map { specimensWithImagesAndInferenceResults ->
            if (sessionId == null) emptyMap()
            else specimensWithImagesAndInferenceResults.associate { specimenWithImagesAndInferenceResults ->
                specimenWithImagesAndInferenceResults.specimen.id to
                        specimenRepository.getSessionUnitIdForSpecimen(
                            specimenId = specimenWithImagesAndInferenceResults.specimen.id,
                            sessionId = sessionId,
                        )
            }
        }

    private val _state = MutableStateFlow(CompleteSessionDetailsState())
    val state: StateFlow<CompleteSessionDetailsState> =
        combine(
            _specimensWithImagesAndInferenceResults,
            _sessionUnitIdBySpecimenId,
            _state
        ) { specimensWithImagesAndInferenceResults, sessionUnitIdBySpecimenId, currentState ->
            val filteredSpecimensWithImagesAndInferenceResults =
                if (currentState.searchQuery.isBlank()) {
                    specimensWithImagesAndInferenceResults
                } else {
                    specimensWithImagesAndInferenceResults.filter { specimenWithImagesAndInferenceResults ->
                        val fieldsForSearch = buildList {
                            add(specimenWithImagesAndInferenceResults.specimen.id)
                            specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.forEach { imageAndInferenceResult ->
                                add(imageAndInferenceResult.specimenImage.species)
                                add(imageAndInferenceResult.specimenImage.sex)
                                add(imageAndInferenceResult.specimenImage.abdomenStatus)
                                add(
                                    imageAndInferenceResult.specimenImage.metadataUploadStatus.displayText(
                                        context
                                    )
                                )
                                add(
                                    imageAndInferenceResult.specimenImage.imageUploadStatus.displayText(
                                        context
                                    )
                                )
                            }
                        }
                        SearchUtils.matchesQuery(currentState.searchQuery, fieldsForSearch)
                    }
                }

            currentState.copy(
                specimensWithImagesAndInferenceResults = filteredSpecimensWithImagesAndInferenceResults,
                sessionUnitIdBySpecimenId = sessionUnitIdBySpecimenId,
            )
        }
            .onStart { loadCompleteSessionDetails() }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                CompleteSessionDetailsState()
            )

    private val _events = Channel<CompleteSessionDetailsEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: CompleteSessionDetailsAction) {
        viewModelScope.launch {
            when (action) {
                CompleteSessionDetailsAction.ReturnToCompleteSessionListScreen -> {
                    _events.send(CompleteSessionDetailsEvent.NavigateBackToCompleteSessionListScreen)
                }

                is CompleteSessionDetailsAction.ChangeSelectedTab -> {
                    _state.update { it.copy(selectedTab = action.selectedTab) }
                }

                is CompleteSessionDetailsAction.UpdateSearchQuery -> {
                    _state.update { it.copy(searchQuery = action.searchQuery) }
                }

                CompleteSessionDetailsAction.ShowSearchTooltipDialog -> {
                    _state.update { it.copy(isSearchTooltipVisible = true) }
                }

                CompleteSessionDetailsAction.HideSearchTooltipDialog -> {
                    _state.update { it.copy(isSearchTooltipVisible = false) }
                }
            }
        }
    }

    private fun loadCompleteSessionDetails() {
        viewModelScope.launch {
            if (sessionId == null) {
                emitError(CompleteSessionDetailsError.SESSION_NOT_FOUND)
                return@launch
            }

            val sessionAndSite = sessionRepository.getSessionAndSiteById(sessionId)
            val sessionAndSurveillanceForm =
                sessionRepository.getSessionAndSurveillanceFormById(sessionId)

            val session = sessionAndSite?.session
            val site = sessionAndSite?.site
            val surveillanceForm = sessionAndSurveillanceForm?.surveillanceForm

            if (session == null) {
                emitError(CompleteSessionDetailsError.SESSION_NOT_FOUND)
                return@launch
            }

            if (site == null) {
                emitError(CompleteSessionDetailsError.SITE_NOT_FOUND)
                return@launch
            }

            val programId = deviceCache.getProgramId()
            if (programId == null) {
                emitError(CompleteSessionDetailsError.PROGRAM_NOT_FOUND)
                return@launch
            }
            val program = programRepository.getProgramById(programId)
            if (program == null) {
                emitError(CompleteSessionDetailsError.PROGRAM_NOT_FOUND)
                return@launch
            }
            val formVersion = program.formVersion
            if (formVersion == null) {
                emitError(CompleteSessionDetailsError.FORM_NOT_FOUND)
                return@launch
            }
            val form = formRepository.getFormByVersion(formVersion)
            if (form == null) {
                emitError(CompleteSessionDetailsError.FORM_NOT_FOUND)
                return@launch
            }

            val allFormQuestions =
                formQuestionRepository.getQuestionsByFormIdAndScope(form.id, answerScope = null)
            val formQuestionsById = allFormQuestions.associateBy { it.id }

            val sessionScopedAnswersByQuestionId =
                formAnswerRepository.getSessionScopedFormAnswers(sessionId)
            val sessionScopedFormAnswersAndQuestions = sessionScopedAnswersByQuestionId
                .mapNotNull { (questionId, formAnswer) ->
                    formQuestionsById[questionId]?.let { formQuestion ->
                        FormAnswerAndQuestion(answer = formAnswer, question = formQuestion)
                    }
                }
                .sortedBy { it.question.order }

            val sessionUnits = sessionUnitRepository
                .getSessionUnitsForSession(sessionId)
                .sortedBy { it.unitOrder }

            val sessionUnitAnswersAndQuestionsByUnitId =
                mutableMapOf<UUID, List<FormAnswerAndQuestion>>()
            val bucketNameBySessionUnitId = mutableMapOf<UUID, String>()

            sessionUnits.forEach { sessionUnit ->
                val answersByQuestionId =
                    formAnswerRepository.getSessionUnitScopedFormAnswers(sessionUnit.localId)

                sessionUnitAnswersAndQuestionsByUnitId[sessionUnit.localId] = answersByQuestionId
                    .mapNotNull { (questionId, formAnswer) ->
                        formQuestionsById[questionId]?.let { formQuestion ->
                            FormAnswerAndQuestion(answer = formAnswer, question = formQuestion)
                        }
                    }
                    .sortedBy { it.question.order }

                val derivedBucketName = CollectionBatchIdentityResolver.deriveBucketName(
                    formQuestions = allFormQuestions,
                    answersByQuestionId = answersByQuestionId,
                )
                bucketNameBySessionUnitId[sessionUnit.localId] = derivedBucketName
                    .takeIf { it.isNotBlank() }
                    ?: "Batch ${sessionUnit.unitOrder}"
            }

            _state.update {
                it.copy(
                    session = session,
                    site = site,
                    surveillanceForm = surveillanceForm,
                    form = form,
                    sessionScopedFormAnswersAndQuestions = sessionScopedFormAnswersAndQuestions,
                    sessionUnits = sessionUnits,
                    sessionUnitAnswersAndQuestionsByUnitId = sessionUnitAnswersAndQuestionsByUnitId,
                    bucketNameBySessionUnitId = bucketNameBySessionUnitId,
                )
            }
        }
    }
}
