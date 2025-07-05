package com.vci.vectorcamapp.incomplete_session.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.presentation.base.BaseViewModel
import com.vci.vectorcamapp.core.presentation.util.error.collectEmptyStateError
import com.vci.vectorcamapp.incomplete_session.domain.util.IncompleteSessionError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncompleteSessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val currentSessionCache: CurrentSessionCache
) : BaseViewModel() {

    private val _incompleteSessions = sessionRepository.observeIncompleteSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    private val _state = MutableStateFlow(IncompleteSessionState())
    val state = combine(_incompleteSessions, _state) { incompleteSessions, state ->
        state.copy(
            sessions = incompleteSessions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), IncompleteSessionState())

    private val _events = Channel<IncompleteSessionEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.collectEmptyStateError(
            flow = _incompleteSessions,
            emitError = { emitError(IncompleteSessionError.NO_INCOMPLETE_SESSIONS, context) }
        )
    }

    fun onAction(action: IncompleteSessionAction) {
        viewModelScope.launch {
            when (action) {
                is IncompleteSessionAction.ResumeSession -> {
                    try {
                        val relation = sessionRepository.getSessionAndSiteById(action.sessionId)
                        if (relation != null) {
                            currentSessionCache.saveSession(relation.session, relation.site.id)
                            _events.send(IncompleteSessionEvent.NavigateToSurveillanceForm)
                        } else {
                            emitError(IncompleteSessionError.SESSION_NOT_FOUND, context)
                        }
                    } catch (e: Exception) {
                        emitError(IncompleteSessionError.SESSION_RETRIEVAL_FAILED, context)
                    }
                }
            }
        }
    }
}
