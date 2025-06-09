package com.vci.vectorcamapp.complete_session.list.presentation

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
class CompleteSessionListViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {

    private val _completeSessions = sessionRepository.observeCompleteSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    private val _state = MutableStateFlow(CompleteSessionListState())
    val state = combine(_completeSessions, _state) { completeSessions, state ->
        state.copy(
            sessions = completeSessions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CompleteSessionListState())
}
