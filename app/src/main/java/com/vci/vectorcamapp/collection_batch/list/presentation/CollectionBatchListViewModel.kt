package com.vci.vectorcamapp.collection_batch.list.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vci.vectorcamapp.collection_batch.domain.util.CollectionBatchIdentityResolver
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CollectionBatchListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceCache: DeviceCache,
    private val currentSessionCache: CurrentSessionCache,
    private val sessionRepository: SessionRepository,
    private val sessionUnitRepository: SessionUnitRepository,
    private val programRepository: ProgramRepository,
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository,
    private val formAnswerRepository: FormAnswerRepository,
    private val workManagerRepository: WorkManagerRepository,
    errorMessageEmitter: ErrorMessageEmitter
) : CoreViewModel(errorMessageEmitter) {

    private val destination = savedStateHandle.toRoute<Destination.CollectionBatchList>()
    private val sessionId: UUID = UUID.fromString(destination.sessionId)

    private val _sessionUnits = sessionUnitRepository.observeSessionUnitsForSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _existingAnswersBySessionUnitId =
        formAnswerRepository.observeSessionUnitScopedFormAnswersBySessionId(sessionId)

    private val _state = MutableStateFlow(CollectionBatchListState(sessionId = sessionId))
    val state = combine(
        _sessionUnits,
        _existingAnswersBySessionUnitId,
        _state,
    ) { sessionUnits, existingAnswersBySessionUnitId, state ->
        val formQuestions = loadSessionUnitFormQuestions()

        state.copy(
            isLoading = false,
            sessionUnits = sessionUnits,
            specimenCountsBySessionUnitId = sessionUnits.associate { sessionUnit ->
                sessionUnit.localId to sessionUnitRepository.countSpecimensForSessionUnit(sessionUnit.localId)
            },
            bucketNamesBySessionUnitId = sessionUnits.associate { sessionUnit ->
                sessionUnit.localId to CollectionBatchIdentityResolver.deriveBucketName(
                    formQuestions = formQuestions,
                    answersByQuestionId = existingAnswersBySessionUnitId[sessionUnit.localId].orEmpty(),
                )
            },
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000L), CollectionBatchListState(sessionId = sessionId)
    )

    private val _events = Channel<CollectionBatchListEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: CollectionBatchListAction) {
        viewModelScope.launch {
            when (action) {
                CollectionBatchListAction.AddCollectionBatch -> {
                    _events.send(
                        CollectionBatchListEvent.NavigateToCollectionBatchForm(
                            sessionId = sessionId,
                            sessionUnitId = null
                        )
                    )
                }

                is CollectionBatchListAction.EditCollectionBatch -> {
                    _events.send(
                        CollectionBatchListEvent.NavigateToCollectionBatchForm(
                            sessionId = sessionId,
                            sessionUnitId = action.sessionUnitId
                        )
                    )
                }

                CollectionBatchListAction.OpenSubmitDialog -> {
                    _state.update { it.copy(isSubmitDialogVisible = true) }
                }

                CollectionBatchListAction.DismissSubmitDialog -> {
                    _state.update { it.copy(isSubmitDialogVisible = false) }
                }

                CollectionBatchListAction.SaveSessionProgress -> {
                    _state.update { it.copy(isSubmitDialogVisible = false) }
                    currentSessionCache.clearSession()
                    _events.send(CollectionBatchListEvent.NavigateBackToLandingScreen)
                }

                CollectionBatchListAction.ConfirmSubmitSession -> {
                    _state.update { it.copy(isSubmitDialogVisible = false) }
                    val currentSession = currentSessionCache.getSession()
                    val currentSessionSiteId = currentSessionCache.getSiteId()

                    if (currentSession == null || currentSessionSiteId == null) {
                        _events.send(CollectionBatchListEvent.NavigateBackToLandingScreen)
                        return@launch
                    }

                    val success = sessionRepository.markSessionAsComplete(currentSession.localId)
                    if (success) {
                        workManagerRepository.enqueueSessionUpload(
                            currentSession.localId, currentSessionSiteId
                        )
                        currentSessionCache.clearSession()
                        _events.send(CollectionBatchListEvent.NavigateBackToLandingScreen)
                    }
                }
            }
        }
    }

    private suspend fun loadSessionUnitFormQuestions(): List<FormQuestion> {
        val programId = deviceCache.getProgramId() ?: return emptyList()
        val program = programRepository.getProgramById(programId) ?: return emptyList()
        val form = program.formVersion?.let { formRepository.getFormByVersion(it) } ?: return emptyList()
        return formQuestionRepository.getQuestionsByFormIdAndScope(
            form.id,
            FormQuestionScope.SESSION_UNIT,
        )
    }
}