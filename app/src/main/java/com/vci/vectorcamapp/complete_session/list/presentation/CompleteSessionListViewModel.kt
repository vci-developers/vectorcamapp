package com.vci.vectorcamapp.complete_session.list.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext override val context: Context,
    sessionRepository: SessionRepository
) : CoreViewModel() {

    private val _completeSessionsAndSites = sessionRepository.observeCompleteSessionsAndSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _state = MutableStateFlow(CompleteSessionListState())
    val state = combine(_completeSessionsAndSites, _state) { completeSessionsAndSites, state ->
        state.copy(
            sessionsAndSites = completeSessionsAndSites
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CompleteSessionListState())

    private val _events = Channel<CompleteSessionListEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: CompleteSessionListAction) {
        when (action) {
            is CompleteSessionListAction.ViewCompleteSessionDetails -> {
                viewModelScope.launch {
                    _events.send(CompleteSessionListEvent.NavigateToCompleteSessionDetails(action.sessionId))
                }
            }
        }
    }
}
