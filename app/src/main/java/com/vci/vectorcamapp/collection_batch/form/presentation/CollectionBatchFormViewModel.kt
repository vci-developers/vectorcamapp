package com.vci.vectorcamapp.collection_batch.form.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.core.domain.model.FormAnswer
import com.vci.vectorcamapp.core.domain.model.enums.FormQuestionScope
import com.vci.vectorcamapp.core.domain.repository.FormQuestionRepository
import com.vci.vectorcamapp.core.domain.repository.FormRepository
import com.vci.vectorcamapp.core.domain.repository.ProgramRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.intake.domain.util.FormQuestionPrerequisiteEvaluator
import com.vci.vectorcamapp.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val programRepository: ProgramRepository,
    private val formRepository: FormRepository,
    private val formQuestionRepository: FormQuestionRepository,
    errorMessageEmitter: ErrorMessageEmitter
) : CoreViewModel(errorMessageEmitter) {

    private val destination = savedStateHandle.toRoute<Destination.CollectionBatchForm>()
    private val sessionId: UUID = UUID.fromString(destination.sessionId)
    private val sessionUnitId: UUID? = destination.sessionUnitId?.let(UUID::fromString)

    private val _state = MutableStateFlow(
        CollectionBatchFormState(
            sessionId = sessionId,
            sessionUnitId = sessionUnitId
        )
    )
    val state: StateFlow<CollectionBatchFormState> = _state.onStart {
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

            _state.update {
                it.copy(
                    isLoading = false,
                    formQuestions = formQuestions,
                    formAnswersByQuestionId = formQuestions.associate { question ->
                        question.id to FormAnswer(
                            localId = UUID.randomUUID(),
                            remoteId = null,
                            value = when (question.type) {
                                "boolean" -> "false"; else -> ""
                            },
                            dataType = question.type,
                            submittedAt = 0L
                        )
                    },
                )
            }
        }
    }
}