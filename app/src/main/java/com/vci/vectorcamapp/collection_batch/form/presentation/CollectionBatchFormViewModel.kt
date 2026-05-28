package com.vci.vectorcamapp.collection_batch.form.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vci.vectorcamapp.collection_batch.domain.util.CollectionBatchIdentityValidator
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.data.room.dao.FormAnswerDao
import com.vci.vectorcamapp.core.data.room.dao.SessionUnitDao
import com.vci.vectorcamapp.core.data.room.entities.FormAnswerEntity
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.AnswerScopes
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.FormQuestion
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.intake.domain.util.FormValidationError
import com.vci.vectorcamapp.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CollectionBatchFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionUnitRepository: SessionUnitRepository,
    private val sessionUnitDao: SessionUnitDao,
    private val formAnswerDao: FormAnswerDao,
    private val formAnswerRepository: FormAnswerRepository,
    private val deviceCache: DeviceCache,
    private val programRepository: ProgramRepository,
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

    private val destination = savedStateHandle.toRoute<Destination.CollectionBatchForm>()
    private val sessionId = UUID.fromString(destination.sessionId)

    @Inject
    lateinit var transactionHelper: TransactionHelper

    private val _state = MutableStateFlow(
        CollectionBatchFormState(
            sessionId = destination.sessionId,
            editingUnitId = destination.unitId,
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<CollectionBatchFormEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadForm()
    }

    private fun loadForm() {
        viewModelScope.launch {
            val allQuestions = loadQuestions()
            val unitQuestions = allQuestions.filter { it.answerScope == AnswerScopes.SESSION_UNIT }

            val identityQuestions = unitQuestions
                .filter { it.isUnitIdentityComponent }
                .sortedBy { it.id }
            val otherUnitQuestions = unitQuestions
                .filter { !it.isUnitIdentityComponent }
                .sortedBy { it.order ?: Int.MAX_VALUE }

            val existingAnswers: Map<Int, String> = if (destination.unitId != null) {
                val unitId = UUID.fromString(destination.unitId)
                formAnswerDao.getFormAnswersBySessionUnitId(unitId)
                    .associate { it.questionId to it.value }
            } else {
                emptyMap()
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    identityQuestions = identityQuestions,
                    otherUnitQuestions = otherUnitQuestions,
                    answers = existingAnswers,
                )
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

    fun onAction(action: CollectionBatchFormAction) {
        viewModelScope.launch {
            when (action) {
                CollectionBatchFormAction.ReturnToPreviousScreen ->
                    _events.send(CollectionBatchFormEvent.NavigateBackToPreviousScreen)

                is CollectionBatchFormAction.EnterAnswer -> {
                    _state.update { s ->
                        s.copy(
                            answers = s.answers + (action.questionId to action.value),
                            errorsByQuestionId = s.errorsByQuestionId - action.questionId,
                        )
                    }
                }

                CollectionBatchFormAction.Confirm -> confirmBatch()
            }
        }
    }

    private suspend fun confirmBatch() {
        val current = _state.value
        val allUnitQuestions = current.identityQuestions + current.otherUnitQuestions

        // Validate required questions
        val newErrors = allUnitQuestions
            .filter { it.required && current.answers[it.id].isNullOrBlank() }
            .associate { it.id to FormValidationError.INVALID_FORM_ANSWER }

        if (newErrors.isNotEmpty()) {
            _state.update { it.copy(errorsByQuestionId = newErrors) }
            return
        }

        // Duplicate identity check
        val existingUnitsWithAnswers = sessionUnitDao.getSessionUnitsWithAnswersForSession(sessionId)
        val existingAnswersByUnitId: Map<UUID, Map<Int, String>> = existingUnitsWithAnswers
            .associate { rel ->
                rel.sessionUnitEntity.localId to rel.answerEntities.associate { it.questionId to it.value }
            }

        val editingUnitId = current.editingUnitId?.let { UUID.fromString(it) }
        val allQuestions = current.identityQuestions + current.otherUnitQuestions
        val isDuplicate = CollectionBatchIdentityValidator.wouldDuplicate(
            questions = allQuestions,
            draftAnswers = current.answers,
            existingUnits = existingAnswersByUnitId,
            editingUnitId = editingUnitId,
        )

        if (isDuplicate) {
            _state.update {
                it.copy(
                    duplicateIdentityError = "A collection batch with this identity already exists. Please change one of the highlighted fields."
                )
            }
            return
        }

        // Persist the unit and its answers atomically
        val now = Instant.now().toEpochMilli()
        val unit: SessionUnit = if (editingUnitId == null) {
            val nextOrder = sessionUnitRepository.getNextUnitOrder(sessionId)
            SessionUnit(
                localId = UUID.randomUUID(),
                sessionId = sessionId,
                remoteId = null,
                unitOrder = nextOrder,
                createdAt = now,
            )
        } else {
            sessionUnitRepository.getSessionUnitById(editingUnitId)
                ?: SessionUnit(
                    localId = editingUnitId,
                    sessionId = sessionId,
                    remoteId = null,
                    unitOrder = sessionUnitRepository.getNextUnitOrder(sessionId),
                    createdAt = now,
                )
        }

        transactionHelper.runAsTransaction {
            sessionUnitRepository.upsertSessionUnit(unit)
            // Delete old answers for this unit before re-inserting so edits are clean
            formAnswerDao.deleteFormAnswersForSessionUnit(unit.localId)
            allUnitQuestions.forEach { q ->
                val answerValue = current.answers[q.id].orEmpty()
                if (answerValue.isNotBlank()) {
                    formAnswerDao.upsertFormAnswer(
                        FormAnswer(
                            localId = UUID.randomUUID(),
                            remoteId = null,
                            value = answerValue,
                            dataType = q.type,
                            submittedAt = now,
                            sessionUnitId = unit.localId,
                        ).toEntity(sessionId, q.id, unit.localId)
                    )
                }
            }
        }

        _events.send(CollectionBatchFormEvent.NavigateToImagingScreen(unit.localId.toString()))
    }
}
