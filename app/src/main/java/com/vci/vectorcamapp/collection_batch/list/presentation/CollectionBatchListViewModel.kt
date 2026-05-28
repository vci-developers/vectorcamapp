package com.vci.vectorcamapp.collection_batch.list.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vci.vectorcamapp.collection_batch.domain.util.CollectionBatchIdentityResolver
import com.vci.vectorcamapp.core.data.room.dao.SessionUnitDao
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.FormQuestion
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
    private val sessionUnitRepository: SessionUnitRepository,
    private val sessionUnitDao: SessionUnitDao,
    private val currentSessionCache: CurrentSessionCache,
    private val deviceCache: DeviceCache,
    private val programRepository: ProgramRepository,
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository,
    private val sessionRepository: SessionRepository,
    private val workRepository: WorkManagerRepository,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

    private val destination = savedStateHandle.toRoute<Destination.CollectionBatchList>()
    private val sessionId = UUID.fromString(destination.sessionId)

    private val _unitScoped = MutableStateFlow(emptyList<CollectionBatchCardData>())
    private val _isLoading = MutableStateFlow(true)

    val state = combine(_unitScoped, _isLoading) { units, loading ->
        CollectionBatchListState(
            isLoading = loading,
            sessionId = destination.sessionId,
            units = units,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = CollectionBatchListState(sessionId = destination.sessionId),
    )

    private val _events = Channel<CollectionBatchListEvent>()
    val events = _events.receiveAsFlow()

    init {
        observeUnits()
    }

    private fun observeUnits() {
        viewModelScope.launch {
            val questions = loadQuestions()
            sessionUnitRepository.observeSessionUnitsForSession(sessionId).collect { units ->
                val cardData = units.map { unit ->
                    val withAnswers = sessionUnitDao.getSessionUnitWithAnswers(unit.localId)
                    val answersByQuestionId = withAnswers?.answerEntities
                        ?.associate { it.questionId to it.value }
                        ?: emptyMap()
                    val bucketName = CollectionBatchIdentityResolver
                        .deriveBucketName(questions, answersByQuestionId)
                        .ifBlank { "Batch ${unit.unitOrder}" }
                    val specimenCount = sessionUnitRepository.countSpecimensForUnit(unit.localId)
                    CollectionBatchCardData(
                        localId = unit.localId,
                        unitOrder = unit.unitOrder,
                        bucketName = bucketName,
                        specimenCount = specimenCount,
                        createdAt = unit.createdAt,
                        canDelete = specimenCount == 0,
                    )
                }
                _unitScoped.update { cardData }
                _isLoading.update { false }
            }
        }
    }

    private suspend fun loadQuestions(): List<FormQuestion> {
        val programId = deviceCache.getProgramId() ?: return emptyList()
        val program = programRepository.getProgramById(programId) ?: return emptyList()
        val formVersion = program.formVersion ?: return emptyList()
        val form = formRepository.getFormByVersion(formVersion) ?: return emptyList()
        return formQuestionRepository.getQuestionsByFormId(form.id)
    }

    fun onAction(action: CollectionBatchListAction) {
        viewModelScope.launch {
            when (action) {
                CollectionBatchListAction.ReturnToPreviousScreen ->
                    _events.send(CollectionBatchListEvent.NavigateBackToLandingScreen)

                CollectionBatchListAction.AddCollectionBatch ->
                    _events.send(
                        CollectionBatchListEvent.NavigateToCollectionBatchForm(
                            sessionId = destination.sessionId,
                            unitId = null,
                        )
                    )

                is CollectionBatchListAction.OpenCollectionBatchImaging ->
                    _events.send(
                        CollectionBatchListEvent.NavigateToImaging(action.unitId.toString())
                    )

                is CollectionBatchListAction.EditCollectionBatch ->
                    _events.send(
                        CollectionBatchListEvent.NavigateToCollectionBatchForm(
                            sessionId = destination.sessionId,
                            unitId = action.unitId.toString(),
                        )
                    )

                is CollectionBatchListAction.DeleteCollectionBatch -> {
                    val unit = sessionUnitRepository.getSessionUnitById(action.unitId)
                        ?: return@launch
                    val deleted = sessionUnitRepository.deleteSessionUnitIfNoSpecimens(unit)
                    if (!deleted) {
                        emitError(com.vci.vectorcamapp.imaging.domain.util.ImagingError.PROCESSING_ERROR)
                    }
                }

                CollectionBatchListAction.UploadSession -> {
                    val session = currentSessionCache.getSession() ?: return@launch
                    val siteId = currentSessionCache.getSiteId() ?: return@launch
                    val success = sessionRepository.markSessionAsComplete(session.localId)
                    if (success) {
                        workRepository.enqueueSessionUpload(session.localId, siteId)
                        currentSessionCache.clearSession()
                        _events.send(CollectionBatchListEvent.NavigateBackToLandingScreen)
                    }
                }
            }
        }
    }
}
