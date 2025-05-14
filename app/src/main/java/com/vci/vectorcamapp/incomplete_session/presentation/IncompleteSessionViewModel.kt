package com.vci.vectorcamapp.incomplete_session.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class IncompleteSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _incompleteSessions = sessionRepository.observeIncompleteSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    private val _state = MutableStateFlow(IncompleteSessionState())
    val state = combine(_incompleteSessions, _state) { incompleteSessions, state ->
        state.copy(
            sessions = incompleteSessions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), IncompleteSessionState())
}
