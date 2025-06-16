package com.vci.vectorcamapp.complete_session.specimens.presentation

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
class CompleteSessionSpecimensViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CompleteSessionSpecimensState())
    val state: StateFlow<CompleteSessionSpecimensState> = _state

    fun loadSession(sessionId: String) {
        val uuid = try {
            UUID.fromString(sessionId)
        } catch (e: IllegalArgumentException) {
            _state.value = _state.value.copy(
                error = "Invalid session ID"
            )
            return
        }

        viewModelScope.launch {
            sessionRepository.observeSessionWithSpecimens(uuid).collect { result ->
                if (result == null) {
                    _state.value = _state.value.copy(
                        error = "No data found for session"
                    )
                } else {
                    _state.value = _state.value.copy(
                        session = result.session,
                        specimens = result.specimens,
                        error = null
                    )
                }
            }
        }
    }
}
