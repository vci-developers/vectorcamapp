package com.vci.vectorcamapp.collection_batch.form.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vci.vectorcamapp.collection_batch.domain.use_cases.CollectionBatchFormValidationUseCases
import com.vci.vectorcamapp.collection_batch.domain.util.error.CollectionBatchFormError
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.repository.FormAnswerRepository
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.domain.repository.SessionUnitRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.errorOrNull
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
import com.vci.vectorcamapp.navigation.Destination
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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CollectionBatchFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceCache: DeviceCache,
    private val sessionUnitRepository: SessionUnitRepository,
    private val programRepository: ProgramRepository,
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository,
    private val formAnswerRepository: FormAnswerRepository,
    private val collectionBatchFormValidationUseCases: CollectionBatchFormValidationUseCases,
    errorMessageEmitter: ErrorMessageEmitter
) : CoreViewModel(errorMessageEmitter) {

    @Inject
    lateinit var transactionHelper: TransactionHelper

    private val destination = savedStateHandle.toRoute<Destination.CollectionBatchForm>()
    private val sessionId: UUID = UUID.fromString(destination.sessionId)
    private val sessionUnitId: UUID? = destination.sessionUnitId?.let(UUID::fromString)

    private val _state = MutableStateFlow(
        CollectionBatchFormState(
            sessionId = sessionId,
            sessionUnitId = sessionUnitId
        )
    )
    private val _existingAnswersBySessionUnitId =
        formAnswerRepository.observeSessionUnitScopedFormAnswersBySessionId(sessionId)

    val state: StateFlow<CollectionBatchFormState> = combine(
        _state,
        _existingAnswersBySessionUnitId,
    ) { state, existingAnswersBySessionUnitId ->
        val duplicateIdentity = collectionBatchFormValidationUseCases
            .validateCollectionBatchIdentity(
                formQuestions = state.formQuestions,
                draftAnswersByQuestionId = state.formAnswersByQuestionId,
                existingAnswersBySessionUnitId = existingAnswersBySessionUnitId,
                editingSessionUnitId = sessionUnitId,
            ).errorOrNull()
        state.copy(
            collectionBatchFormErrors = state.collectionBatchFormErrors.copy(
                duplicateIdentity = duplicateIdentity
            )
        )
    }.onStart {
        loadFormDetails()
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionBatchFormState(
            sessionId = sessionId,
            sessionUnitId = sessionUnitId
        )
    )

    private val _events = Channel<CollectionBatchFormEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: CollectionBatchFormAction) {
        viewModelScope.launch {
            when (action) {
                CollectionBatchFormAction.ReturnToCollectionBatchListScreen -> {
                    _events.send(CollectionBatchFormEvent.NavigateBackToCollectionBatchListScreen)
                }

                is CollectionBatchFormAction.UpdateFormAnswer -> {
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
                            if (question.id != action.questionId &&
                                !FormQuestionPrerequisiteEvaluator.evaluate(
                                    question.prerequisite,
                                    answerMap
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

                CollectionBatchFormAction.SubmitSessionUnitForm -> {
                    val formQuestions = _state.value.formQuestions
                    val formAnswersByQuestionId = _state.value.formAnswersByQuestionId

                    val formAnswersResult =
                        collectionBatchFormValidationUseCases.validateFormAnswers(
                            formQuestions, formAnswersByQuestionId,
                        )

                    _state.update {
                        it.copy(
                            collectionBatchFormErrors = it.collectionBatchFormErrors.copy(
                                formAnswerErrors = formAnswersResult.mapValues { (_, result) -> result.errorOrNull() },
                            )
                        )
                    }

                    val hasFormAnswersError = formAnswersResult.values.any { it is Result.Error }
                    val hasDuplicateIdentityError =
                        state.value.collectionBatchFormErrors.duplicateIdentity != null

                    if (hasFormAnswersError) {
                        emitError(CollectionBatchFormError.FORM_INVALID)
                        return@launch
                    } else if (hasDuplicateIdentityError) {
                        emitError(CollectionBatchFormError.DUPLICATE_IDENTITY)
                        return@launch
                    } else {
                        val existingSessionUnit = sessionUnitId?.let {
                            sessionUnitRepository.getSessionUnitById(it)
                        }
                        val effectiveSessionUnit = existingSessionUnit ?: SessionUnit(
                            localId = sessionUnitId ?: UUID.randomUUID(),
                            remoteId = null,
                            unitOrder = sessionUnitRepository.getMaxSessionUnitOrderForSession(
                                sessionId
                            ) + 1,
                            createdAt = System.currentTimeMillis(),
                        )

                        val success = transactionHelper.runAsTransaction {
                            val sessionUnitResult = sessionUnitRepository.upsertSessionUnit(
                                effectiveSessionUnit,
                                sessionId
                            )
                            sessionUnitResult.onError { error ->
                                emitError(error)
                                return@runAsTransaction false
                            }

                            for ((questionId, answer) in formAnswersByQuestionId) {
                                formAnswerRepository.upsertFormAnswer(
                                    answer, sessionId, effectiveSessionUnit.localId, questionId,
                                ).onError { error ->
                                    emitError(error)
                                    return@runAsTransaction false
                                }
                            }

                            true
                        }

                        if (success) {
                            _events.send(
                                CollectionBatchFormEvent.NavigateToImagingScreen(
                                    effectiveSessionUnit.localId
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadFormDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val programId = deviceCache.getProgramId() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val program = programRepository.getProgramById(programId) ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            val form = program.formVersion?.let { formRepository.getFormByVersion(it) }
            val formQuestions = form?.let {
                formQuestionRepository.getQuestionsByFormIdAndScope(
                    it.id,
                    FormQuestionScope.SESSION_UNIT
                )
            }.orEmpty()

            val savedFormAnswers = sessionUnitId?.let {
                formAnswerRepository.getFormAnswersBySessionUnitId(it)
            } ?: emptyMap()

            _state.update {
                it.copy(
                    isLoading = false,
                    formQuestions = formQuestions,
                    formAnswersByQuestionId = formQuestions.associate { question ->
                        question.id to (savedFormAnswers[question.id] ?: FormAnswer(
                            localId = UUID.randomUUID(),
                            remoteId = null,
                            value = when (question.type) {
                                "boolean" -> "false"; else -> ""
                            },
                            dataType = question.type,
                            submittedAt = 0L
                        ))
                    },
                )
            }
        }
    }
}