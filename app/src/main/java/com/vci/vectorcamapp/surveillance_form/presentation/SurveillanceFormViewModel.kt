package com.vci.vectorcamapp.surveillance_form.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SurveillanceFormRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.errorOrNull
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import com.vci.vectorcamapp.surveillance_form.domain.use_cases.ValidationUseCases
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
class SurveillanceFormViewModel @Inject constructor(
    private val transactionHelper: TransactionHelper,
    private val validationUseCases: ValidationUseCases,
    private val currentSessionCache: CurrentSessionCache,
    private val surveillanceFormRepository: SurveillanceFormRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SurveillanceFormState())
    val state: StateFlow<SurveillanceFormState> = _state.onStart {
        getLocation()
        loadSavedForm()
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000L), SurveillanceFormState()
    )

    private val _events = Channel<SurveillanceFormEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: SurveillanceFormAction) {
        viewModelScope.launch {
            when (action) {
                SurveillanceFormAction.SaveSessionProgress -> {
                    currentSessionCache.clearSession()
                    _events.send(SurveillanceFormEvent.NavigateBackToLandingScreen)
                }

                SurveillanceFormAction.SubmitSurveillanceForm -> {
                    val surveillanceForm = _state.value.surveillanceForm

                    val llinTypeResult =
                        surveillanceForm.llinType?.let { validationUseCases.validateLlinType(it) }
                    val llinBrandResult =
                        surveillanceForm.llinBrand?.let { validationUseCases.validateLlinBrand(it) }

                    _state.update {
                        it.copy(
                            surveillanceFormErrors = it.surveillanceFormErrors.copy(
                                llinType = llinTypeResult?.errorOrNull(),
                                llinBrand = llinBrandResult?.errorOrNull()
                            )
                        )
                    }

                    val hasError = listOf(
                        llinTypeResult,
                        llinBrandResult
                    ).any { it is Result.Error }

                    if (!hasError) {
                        // TODO: MODIFY WHEN FORM FIELDS ARE CREATED
                        val newSession = Session(
                            localId = UUID.randomUUID(),
                            remoteId = null,
                            houseNumber = "1",
                            collectorTitle = "Student",
                            collectorName = "Queen Victoria II",
                            collectionDate = 0L,
                            collectionMethod = "HLC",
                            specimenCondition = "Dessicated",
                            createdAt = System.currentTimeMillis(),
                            completedAt = null,
                            submittedAt = null,
                            notes = "Fake Session"
                        )

                        val success = transactionHelper.runAsTransaction {
                            val sessionResult = sessionRepository.upsertSession(newSession, 1)
                            val surveillanceFormResult = surveillanceFormRepository.upsertSurveillanceForm(
                                surveillanceForm,
                                newSession.localId
                            )

                            sessionResult.onError { error ->
                                Log.e("ROOM DB ERROR", "Session Error: $error")
                            }

                            surveillanceFormResult.onError { error ->
                                Log.e("ROOM DB ERROR", "Surveillance Form Error: $error")
                            }

                            (sessionResult !is Result.Error) && (surveillanceFormResult !is Result.Error)
                        }

                        if (success) {
                            currentSessionCache.saveSession(newSession, 1)
                            _events.send(SurveillanceFormEvent.NavigateToImagingScreen)
                        }
                    }
                }

                is SurveillanceFormAction.EnterNumPeopleSleptInHouse -> {
                    val numPeopleSleptInHouse =
                        if (action.count.isBlank()) 0 else action.count.toIntOrNull()
                    numPeopleSleptInHouse?.let { count ->
                        _state.update {
                            it.copy(
                                surveillanceForm = it.surveillanceForm.copy(
                                    numPeopleSleptInHouse = count
                                )
                            )
                        }
                    }
                }

                is SurveillanceFormAction.ToggleIrsConducted -> {
                    val wasIrsConducted = action.isChecked
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                wasIrsConducted = wasIrsConducted
                            )
                        )
                    }
                    if (!wasIrsConducted) {
                        _state.update {
                            it.copy(
                                surveillanceForm = it.surveillanceForm.copy(
                                    monthsSinceIrs = null
                                )
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                surveillanceForm = it.surveillanceForm.copy(
                                    monthsSinceIrs = 0
                                )
                            )
                        }
                    }
                }

                is SurveillanceFormAction.EnterMonthsSinceIrs -> {
                    val monthsSinceIrs =
                        if (action.count.isBlank()) 0 else action.count.toIntOrNull()
                    monthsSinceIrs?.let { count ->
                        _state.update {
                            it.copy(
                                surveillanceForm = it.surveillanceForm.copy(
                                    monthsSinceIrs = count
                                )
                            )
                        }
                    }
                }

                is SurveillanceFormAction.EnterNumLlinsAvailable -> {
                    val numLlinsAvailable =
                        if (action.count.isBlank()) 0 else action.count.toIntOrNull()
                    numLlinsAvailable?.let { count ->
                        _state.update {
                            it.copy(
                                surveillanceForm = it.surveillanceForm.copy(
                                    numLlinsAvailable = count
                                )
                            )
                        }
                        if (numLlinsAvailable == 0) {
                            _state.update {
                                it.copy(
                                    surveillanceForm = it.surveillanceForm.copy(
                                        llinType = null,
                                        llinBrand = null,
                                        numPeopleSleptUnderLlin = null
                                    )
                                )
                            }
                        } else {
                            _state.update {
                                it.copy(
                                    surveillanceForm = it.surveillanceForm.copy(
                                        llinType = "",
                                        llinBrand = "",
                                        numPeopleSleptUnderLlin = 0
                                    )
                                )
                            }
                        }
                    }
                }

                is SurveillanceFormAction.SelectLlinType -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                llinType = action.option.label
                            )
                        )
                    }
                }

                is SurveillanceFormAction.SelectLlinBrand -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                llinBrand = action.option.label
                            )
                        )
                    }
                }

                is SurveillanceFormAction.EnterNumPeopleSleptUnderLlin -> {
                    val numPeopleSleptUnderLlin =
                        if (action.count.isBlank()) 0 else action.count.toIntOrNull()
                    numPeopleSleptUnderLlin?.let { count ->
                        _state.update {
                            it.copy(
                                surveillanceForm = it.surveillanceForm.copy(
                                    numPeopleSleptUnderLlin = count
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getLocation() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true
                )
            }
            // FETCH LOCATION DATA HERE
            _state.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    private suspend fun loadSavedForm() {
//        Log.d("SurveillanceFormViewModel", "loadSavedForm")
        val session = currentSessionCache.getSession() ?: return
        val saved = surveillanceFormRepository.getSurveillanceForm(session.id)
//        Log.d("SurveillanceFormViewModel", "loadSavedForm: $session")
        if (saved != null) {
//            Log.d("SurveillanceFormViewModel", "loadSavedForm: $saved")
            _state.update { it.copy(surveillanceForm = saved) }
        }
    }
}
