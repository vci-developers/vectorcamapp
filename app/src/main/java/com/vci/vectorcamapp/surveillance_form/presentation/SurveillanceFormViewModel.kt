package com.vci.vectorcamapp.surveillance_form.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
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
import javax.inject.Inject

@HiltViewModel
class SurveillanceFormViewModel @Inject constructor(
    private val validationUseCases: ValidationUseCases,
    private val currentSessionCache: CurrentSessionCache,
    private val surveillanceFormRepository: SurveillanceFormRepository
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

                    val countryResult = validationUseCases.validateCountry(surveillanceForm.country)
                    val districtResult =
                        validationUseCases.validateDistrict(surveillanceForm.district)
                    val healthCenterResult =
                        validationUseCases.validateHealthCenter(surveillanceForm.healthCenter)
                    val sentinelSiteResult =
                        validationUseCases.validateSentinelSite(surveillanceForm.sentinelSite)
                    val householdNumberResult =
                        validationUseCases.validateHouseholdNumber(surveillanceForm.householdNumber)
                    val collectionDateResult =
                        validationUseCases.validateCollectionDate(surveillanceForm.collectionDate)
                    val collectionMethodResult =
                        validationUseCases.validateCollectionMethod(surveillanceForm.collectionMethod)
                    val collectorNameResult =
                        validationUseCases.validateCollectorName(surveillanceForm.collectorName)
                    val collectorTitleResult =
                        validationUseCases.validateCollectorTitle(surveillanceForm.collectorTitle)
                    val llinTypeResult =
                        surveillanceForm.llinType?.let { validationUseCases.validateLlinType(it) }
                    val llinBrandResult =
                        surveillanceForm.llinBrand?.let { validationUseCases.validateLlinBrand(it) }

                    _state.update {
                        it.copy(
                            surveillanceFormErrors = it.surveillanceFormErrors.copy(
                                country = countryResult.errorOrNull(),
                                district = districtResult.errorOrNull(),
                                healthCenter = healthCenterResult.errorOrNull(),
                                sentinelSite = sentinelSiteResult.errorOrNull(),
                                householdNumber = householdNumberResult.errorOrNull(),
                                collectionDate = collectionDateResult.errorOrNull(),
                                collectionMethod = collectionMethodResult.errorOrNull(),
                                collectorName = collectorNameResult.errorOrNull(),
                                collectorTitle = collectorTitleResult.errorOrNull(),
                                llinType = llinTypeResult?.errorOrNull(),
                                llinBrand = llinBrandResult?.errorOrNull()
                            )
                        )
                    }

                    val hasError = listOf(
                        countryResult,
                        districtResult,
                        healthCenterResult,
                        sentinelSiteResult,
                        householdNumberResult,
                        collectionDateResult,
                        collectionMethodResult,
                        collectorNameResult,
                        collectorTitleResult,
                        llinTypeResult,
                        llinBrandResult
                    ).any { it is Result.Error }

                    if (!hasError) {
                        val session = currentSessionCache.getSession()
                        if (session == null) {
                            _events.send(SurveillanceFormEvent.NavigateBackToLandingScreen)
                            return@launch
                        }
                        surveillanceFormRepository.upsertSurveillanceForm(surveillanceForm, session.id).onSuccess {
                            _events.send(SurveillanceFormEvent.NavigateToImagingScreen)
                        }.onError { error ->
                            Log.e("ROOM DB ERROR", error.toString())
                        }
                    }
                }

                is SurveillanceFormAction.EnterCountry -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                country = action.text
                            )
                        )
                    }
                }

                is SurveillanceFormAction.EnterDistrict -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                district = action.text
                            )
                        )
                    }
                }

                is SurveillanceFormAction.EnterHealthCenter -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                healthCenter = action.text
                            )
                        )
                    }
                }

                is SurveillanceFormAction.EnterSentinelSite -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                sentinelSite = action.text
                            )
                        )
                    }
                }

                is SurveillanceFormAction.EnterHouseholdNumber -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                householdNumber = action.text
                            )
                        )
                    }
                }

                is SurveillanceFormAction.PickCollectionDate -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                collectionDate = action.date
                            )
                        )
                    }
                }

                is SurveillanceFormAction.SelectCollectionMethod -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                collectionMethod = action.option.label
                            )
                        )
                    }
                }

                is SurveillanceFormAction.EnterCollectorName -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                collectorName = action.text
                            )
                        )
                    }
                }

                is SurveillanceFormAction.EnterCollectorTitle -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                collectorTitle = action.text
                            )
                        )
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

                is SurveillanceFormAction.EnterNotes -> {
                    _state.update {
                        it.copy(
                            surveillanceForm = it.surveillanceForm.copy(
                                notes = action.text
                            )
                        )
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
