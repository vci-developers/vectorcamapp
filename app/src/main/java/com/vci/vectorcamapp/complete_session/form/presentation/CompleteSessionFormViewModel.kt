package com.vci.vectorcamapp.complete_session.form.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CompleteSessionFormViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CompleteSessionFormState())
    val state: StateFlow<CompleteSessionFormState> = _state

    fun onAction(action: CompleteSessionFormAction) {
        when (action) {
            is CompleteSessionFormAction.LoadSession -> loadSession(action.sessionId)
        }
    }

    private fun loadSession(sessionId: String) {
        val uuid = try {
            UUID.fromString(sessionId)
        } catch (e: IllegalArgumentException) {
            _state.value = _state.value.copy(
                error = "Invalid session ID"
            )
            return
        }

        viewModelScope.launch {
            val siteResult = sessionRepository.getSessionAndSite(uuid)
            val surveillanceFormResult = sessionRepository.getSessionAndSurveillanceForm(uuid)

            _state.value = when {
                siteResult == null -> _state.value.copy(
                    error = "No site data found for session"
                )

                surveillanceFormResult == null -> _state.value.copy(
                    session = siteResult.session,
                    site = siteResult.site,
                    surveillanceForm = null,
                    error = null
                )

                else -> _state.value.copy(
                    session = siteResult.session,
                    site = siteResult.site,
                    surveillanceForm = surveillanceFormResult.surveillanceForm,
                    error = null
                )
            }
        }
    }
}
