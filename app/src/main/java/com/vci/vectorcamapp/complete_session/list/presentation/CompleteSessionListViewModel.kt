package com.vci.vectorcamapp.complete_session.list.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    private val _events = Channel<CompleteSessionListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: CompleteSessionListAction) {
        when (action) {
            is CompleteSessionListAction.ViewCompleteSessionDetail -> {
                viewModelScope.launch {
                    _events.send(CompleteSessionListEvent.NavigateToCompleteSessionDetail(action.sessionId))
                }
            }
        }
    }
}

